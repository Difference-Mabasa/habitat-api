package com.habitat.api.enums;

/**
 * Who controls a listing.
 *
 * <p>{@code LANDLORD_DIRECT} — the landlord created and runs the
 * listing themselves. No agent fee.
 *
 * <p>{@code AGENT_MANAGED} — an agent placed the mandate. The fee
 * percent is captured on the property as {@code mandateFeePercent}.
 * The full mandate-approval state machine (agent attestation,
 * landlord approval, offline-signature flow) lands in Phase 12 — for
 * now this enum + the fee column are the minimum facts the wizard
 * captures so the listing is real.
 */
public enum ListingMode {
    LANDLORD_DIRECT,
    AGENT_MANAGED
}
