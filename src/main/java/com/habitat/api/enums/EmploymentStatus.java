package com.habitat.api.enums;

/**
 * Self-declared employment status on a tenant Application. Mirrors
 * backroom-api's enum exactly so the apply form copy stays portable.
 *
 * <p>Optional on submit — the apply form treats it as a soft hint to
 * the landlord, not gating.
 *
 * <p>Stored as STRING.
 */
public enum EmploymentStatus {
    EMPLOYED,
    SELF_EMPLOYED,
    STUDENT,
    PENSIONER,
    UNEMPLOYED,
    OTHER
}
