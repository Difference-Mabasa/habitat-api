package com.habitat.api.dto.user;

import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Public-facing shape of the signed-in user. Never include the password
 * hash, refresh token, or anything else that could compromise the account.
 */
@Builder
public record UserMeResponse(
        UUID id,
        String email,
        String firstName,
        String surname,
        Set<Role> roles,
        Role activeRole,
        boolean emailVerified,
        String area,
        OffsetDateTime createdAt
) {
    public static UserMeResponse from(User user) {
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .surname(user.getSurname())
                .roles(Set.copyOf(user.getRoles()))
                .activeRole(user.getActiveRole())
                .emailVerified(user.isEmailVerified())
                .area(user.getArea())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
