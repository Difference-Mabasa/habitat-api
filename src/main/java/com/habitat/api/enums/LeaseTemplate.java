package com.habitat.api.enums;

/**
 * SA-law lease templates the design surfaces in the lease-template
 * picker. Stored as a string column so adding a new template is a
 * code change only.
 */
public enum LeaseTemplate {
    /** Rental Housing Act · Standard 12-month. */
    RHA_STANDARD,
    /** Rental Housing Act · 6-month flexi. */
    RHA_SIX_MONTH,
    /** Single room · simplified lease. */
    RHA_ROOM
}
