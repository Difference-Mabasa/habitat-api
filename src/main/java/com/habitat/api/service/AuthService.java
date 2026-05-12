package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.auth.AuthResponse;
import com.habitat.api.dto.auth.LoginRequest;
import com.habitat.api.dto.auth.RegisterRequest;
import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.UnauthorizedException;
import com.habitat.api.exception.ValidationException;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthResponse register(RegisterRequest req) {
        if (req.role() == null) {
            throw new ValidationException(ErrorMessages.ROLE_NOT_SELF_ASSIGNABLE);
        }
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException(ErrorMessages.EMAIL_ALREADY_REGISTERED);
        }

        User user = User.builder()
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName().trim())
                .roles(Set.of(req.role()))
                .activeRole(req.role())
                .emailVerified(false)
                .area(req.area())
                .build();
        user = userRepository.save(user);
        log.info("user registered: {}", user.getId());

        return tokensFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new UnauthorizedException(ErrorMessages.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(ErrorMessages.INVALID_CREDENTIALS);
        }
        log.info("user signed in: {}", user.getId());
        return tokensFor(user);
    }

    private AuthResponse tokensFor(User user) {
        var access = jwt.issueAccess(user);
        var refresh = jwt.issueRefresh(user);
        return AuthResponse.builder()
                .accessToken(access.token())
                .accessTokenExpiresAt(access.expiresAt())
                .refreshToken(refresh.token())
                .refreshTokenExpiresAt(refresh.expiresAt())
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .roles(user.getRoles())
                .activeRole(user.getActiveRole())
                .build();
    }

    /**
     * ADMIN-only — create a user with privileged roles. The web layer guards
     * this with {@code @PreAuthorize("hasRole('SUPER_ADMIN')")}; service-layer
     * guard is mirrored for defence-in-depth.
     */
    public User createWithRoles(String email, String password, String displayName, Set<Role> roles, Role active) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(ErrorMessages.EMAIL_ALREADY_REGISTERED);
        }
        User user = User.builder()
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName.trim())
                .roles(roles)
                .activeRole(active)
                .emailVerified(true)
                .build();
        return userRepository.save(user);
    }
}
