package com.habitat.api.enums;

/**
 * Typed fields a landlord can request the agent to change before
 * signing the mandate. Drives the "Apply suggested" one-click path
 * on the agent's resolution panel — anything outside this enum
 * goes in the freeform comment instead.
 */
public enum ChangeRequestField {
    FEE,
    SCOPE,
    NOTES
}
