package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.auth.AuthResponse;
import com.habitat.api.dto.auth.LoginRequest;
import com.habitat.api.dto.auth.RegisterRequest;
import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import com.habitat.api.event.UserRegisteredEvent;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.exception.UnauthorizedException;
import com.habitat.api.exception.ValidationException;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.HabitatPrincipal;
import com.habitat.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
    private final TokenBlocklistService blocklist;
    private final ApplicationEventPublisher events;

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
                .firstName(req.firstName().trim())
                .surname(req.surname().trim())
                .roles(Set.of(req.role()))
                .activeRole(req.role())
                .emailVerified(false)
                .area(req.area())
                .build();
        user = userRepository.save(user);
        log.info("user registered: {}", user.getId());

        // Side effects (welcome notification, future welcome email, analytics)
        // listen via @TransactionalEventListener(AFTER_COMMIT) so they only
        // run if the registration actually commits.
        events.publishEvent(new UserRegisteredEvent(user.getId(), user));

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

    /**
     * Verify a refresh token, blocklist its jti, and issue a new access +
     * refresh pair. The old refresh token is single-use — replaying it
     * after refresh yields 401.
     */
    public AuthResponse refresh(String refreshToken) {
        HabitatPrincipal principal = jwt.verifyRefresh(refreshToken);
        if (blocklist.isRevoked(principal.jti())) {
            throw new UnauthorizedException(ErrorMessages.REFRESH_TOKEN_REVOKED);
        }
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));

        // Single-use: revoke the presented refresh before issuing a new pair.
        if (principal.expiresAt() != null) {
            blocklist.revoke(principal.jti(), principal.expiresAt());
        }
        log.info("token refreshed for user: {}", user.getId());
        return tokensFor(user);
    }

    /**
     * Revoke the access token currently authenticating the caller. The
     * caller-supplied refresh token is also revoked when present so the
     * full session ends in one call.
     */
    public void logout(HabitatPrincipal accessPrincipal, String refreshToken) {
        if (accessPrincipal != null && accessPrincipal.expiresAt() != null) {
            blocklist.revoke(accessPrincipal.jti(), accessPrincipal.expiresAt());
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                HabitatPrincipal refreshPrincipal = jwt.verifyRefresh(refreshToken);
                if (refreshPrincipal.expiresAt() != null) {
                    blocklist.revoke(refreshPrincipal.jti(), refreshPrincipal.expiresAt());
                }
            } catch (UnauthorizedException ignored) {
                // refresh was already invalid — fine, nothing to revoke.
            }
        }
        if (accessPrincipal != null) {
            log.info("user signed out: {}", accessPrincipal.userId());
        }
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
                .firstName(user.getFirstName())
                .surname(user.getSurname())
                .roles(user.getRoles())
                .activeRole(user.getActiveRole())
                .build();
    }

    /**
     * ADMIN-only — create a user with privileged roles. The web layer guards
     * this with {@code @PreAuthorize("hasRole('SUPER_ADMIN')")}; service-layer
     * guard is mirrored for defence-in-depth.
     */
    public User createWithRoles(String email, String password, String firstName, String surname, Set<Role> roles, Role active) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(ErrorMessages.EMAIL_ALREADY_REGISTERED);
        }
        User user = User.builder()
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName.trim())
                .surname(surname.trim())
                .roles(roles)
                .activeRole(active)
                .emailVerified(true)
                .build();
        return userRepository.save(user);
    }
}
