package com.habitat.api.dto.auth;

import com.habitat.api.enums.Role;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Builder
public record AuthResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UUID userId,
        String email,
        String firstName,
        String surname,
        Set<Role> roles,
        Role activeRole
) {}
