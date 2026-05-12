package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.user.UserMeResponse;
import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserMeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
        return UserMeResponse.from(user);
    }

    public UserMeResponse switchActiveRole(UUID userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
        if (!user.getRoles().contains(role)) {
            throw new ForbiddenException(ErrorMessages.ROLE_NOT_OWNED);
        }
        if (user.getActiveRole() != role) {
            user.setActiveRole(role);
            userRepository.save(user);
            log.info("user {} switched active role to {}", userId, role);
        }
        return UserMeResponse.from(user);
    }
}
