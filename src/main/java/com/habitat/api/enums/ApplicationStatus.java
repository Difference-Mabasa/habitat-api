package com.habitat.api.enums;

/**
 * Lifecycle state of a tenant {@code Application} against a Unit.
 *
 * <p>The active lifecycle walks left-to-right:
 * SUBMITTED → AWAITING_DOCUMENTS → DOCUMENTS_SUBMITTED → UNDER_REVIEW →
 * APPROVED → INVOICE_SENT → DEPOSIT_PAID → LEASE_GENERATED →
 * LEASE_PENDING_SIGNATURES → COMPLETED. {@link #ON_HOLD} is a parked-but-
 * not-dead state the landlord can put an application into mid-review.
 *
 * <p>Terminal off-ramps: {@link #REJECTED} (landlord), {@link #WITHDRAWN}
 * (tenant), {@link #EXPIRED} (system).
 *
 * <p>Stored as STRING in {@code applications.status}.
 */
public enum ApplicationStatus {
    // ── Active lifecycle ─────────────────────────────────────────────
    SUBMITTED,
    AWAITING_DOCUMENTS,
    DOCUMENTS_SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    ON_HOLD,
    INVOICE_SENT,
    DEPOSIT_PAID,
    LEASE_GENERATED,
    LEASE_PENDING_SIGNATURES,
    COMPLETED,

    // ── Terminal off-ramps ───────────────────────────────────────────
    REJECTED,
    WITHDRAWN,
    EXPIRED
}
