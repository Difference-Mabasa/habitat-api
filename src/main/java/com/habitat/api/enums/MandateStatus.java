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
    ACTIVE,
    REJECTED,
    EXPIRED
}
