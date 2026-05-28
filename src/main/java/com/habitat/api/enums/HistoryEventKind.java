package com.habitat.api.enums;

/**
 * Discriminator for {@link com.habitat.api.dto.mandate.HistoryEventResponse}.
 * Drives the timeline rendering on both the landlord and agent
 * mandate detail screens. Derived server-side from the mandate's
 * audit fields + the change_request rows — no separate event-log
 * table is maintained.
 */
public enum HistoryEventKind {
    ISSUED,
    CHANGES_REQUESTED,
    RESUBMITTED,
    APPROVED,
    REJECTED,
    WITHDRAWN
}
