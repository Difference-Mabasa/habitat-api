package com.habitat.api.security;

import com.habitat.api.enums.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticated principal stored on the Spring Authentication. Populated by
 * JwtAuthenticationFilter from the access token's claims.
 *
 * Carries the jti + expiry so logout / refresh can revoke the exact token
 * via TokenBlocklistService.
 */
public record HabitatPrincipal(
        UUID userId,
        String email,
        Set<Role> roles,
        Role activeRole,
        String jti,
        Instant expiresAt
) {}
