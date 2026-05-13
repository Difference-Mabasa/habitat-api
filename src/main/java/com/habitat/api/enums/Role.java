package com.habitat.api.enums;

/**
 * Auth role. The identity-level role attached to a user account; mirrors
 * backroom-api's {@code UserRole} (their R-1 Role &amp; Ownership Rewrite).
 *
 * "Tenant" and "landlord" are NOT roles — they're computed states from the
 * data model:
 * <ul>
 *   <li>A user is a <em>tenant</em> if they hold a lease.</li>
 *   <li>A user is a <em>landlord</em> if they are the manager of a property.</li>
 * </ul>
 * Both fall under {@link #USER}. Habitat does not distinguish them at the
 * auth layer.
 *
 * Storage: enum values are persisted as STRING in Postgres
 * ({@code @Enumerated(EnumType.STRING)}), never as ORDINAL.
 *
 * Self-registration: {@link #ADMIN} and {@link #SUPER_ADMIN} cannot be
 * self-assigned via /auth/register — see AuthService and the
 * {@code @JsonCreator} role whitelist on RegisterRequest.
 */
public enum Role {
    USER,
    AGENT,
    ADMIN,
    SUPER_ADMIN
}
