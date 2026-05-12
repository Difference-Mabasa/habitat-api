package com.habitat.api.enums;

/**
 * User roles. Mirrors the frontend's `Role` type in habitat-ui's session.ts.
 *
 * Important: enum values are stored as STRING in Postgres
 * ({@code @Enumerated(EnumType.STRING)}). Never as ORDINAL.
 *
 * ADMIN and SUPER_ADMIN cannot be self-assigned via /auth/register —
 * see AuthService and the @JsonDeserialize on RegisterRequest.
 */
public enum Role {
    TENANT,
    LANDLORD,
    AGENT,
    ADMIN,
    SUPER_ADMIN
}
