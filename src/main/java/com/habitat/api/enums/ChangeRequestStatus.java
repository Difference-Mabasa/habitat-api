package com.habitat.api.enums;

/**
 * Lifecycle of a single {@link com.habitat.api.entity.MandateChangeRequest}.
 *
 * <pre>
 *   OPEN       — landlord just requested; awaiting agent action
 *   ADDRESSED  — agent resubmitted (mandate is back at PENDING_LANDLORD_APPROVAL)
 *   WITHDRAWN  — landlord acted (approve / reject) before agent did,
 *                or the agent withdrew the mandate
 * </pre>
 */
public enum ChangeRequestStatus {
    OPEN,
    ADDRESSED,
    WITHDRAWN
}
