package com.habitat.api.security;

import com.habitat.api.enums.Role;

import java.util.Set;
import java.util.UUID;

/**
 * Authenticated principal stored on the Spring Authentication. Populated by
 * JwtAuthenticationFilter from the access token's claims.
 */
public record HabitatPrincipal(
        UUID userId,
        String email,
        Set<Role> roles,
        Role activeRole,
        String jti
) {}
