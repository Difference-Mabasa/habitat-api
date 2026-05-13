package com.habitat.api.enums;

/**
 * Top-level property type — the building or stand the listing represents.
 * Distinct from {@link UnitType}: a property of type APARTMENT_BLOCK
 * contains units of type APARTMENT, etc.
 *
 * Stored as STRING (never ORDINAL).
 */
public enum PropertyType {
    HOUSE,
    APARTMENT_BLOCK,
    TOWNHOUSE_COMPLEX,
    COMPLEX,
    PLOT
}
