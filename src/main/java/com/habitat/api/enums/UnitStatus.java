package com.habitat.api.enums;

/**
 * Per-unit availability. AVAILABLE units are eligible for the public
 * search; everything else hides the unit but not the property.
 */
public enum UnitStatus {
    AVAILABLE,
    OCCUPIED,
    UNDER_MAINTENANCE,
    UNLISTED
}
