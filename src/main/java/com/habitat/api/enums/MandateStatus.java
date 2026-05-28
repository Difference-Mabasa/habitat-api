package com.habitat.api.enums;

/**
 * State machine for an agent-landlord mandate. See
 * {@code db/migration/V29__mandates.sql} for the narrative.
 *
 * <pre>
 *                                    online flow
 *   issue()  →  PENDING_LANDLORD_APPROVAL  → (approve)
 *                                            ↓
 *                                          ACTIVE
 *
 *                                    offline flow
 *   issue()  →  PENDING_OFFLINE_SIGNATURE   → (upload signed)
 *                                            ↓
 *                                      PENDING_AGENT_ACCEPTANCE
 *                                            ↓ (attest)
 *                                          ACTIVE
 * </pre>
 */
public enum MandateStatus {
    PENDING_LANDLORD_APPROVAL,
    PENDING_OFFLINE_SIGNATURE,
    PENDING_AGENT_ACCEPTANCE,
    /** Slice 4: landlord has asked the agent to revise the terms
     *  before signing. The mandate sits here until the agent
     *  resubmits (back to PENDING_LANDLORD_APPROVAL) or withdraws
     *  (forward to REJECTED). */
    CHANGES_REQUESTED,
    ACTIVE,
    REJECTED,
    EXPIRED
}
