package com.habitat.api.event;

import java.util.UUID;

/**
 * Published when an online landlord approves a mandate via
 * {@link com.habitat.api.service.MandateService#approveByLandlord}.
 *
 * <p>The approve transition flips status PENDING_LANDLORD_APPROVAL →
 * ACTIVE, so the listener pushes BOTH a MANDATE_APPROVED notice to
 * the agent (so they know the landlord acted) AND a MANDATE_ACTIVE
 * acknowledgement to both parties (the terminal-state confirmation).
 * Keeping them separate makes a future "landlord approved but agent
 * still has more to do" flow trivial to add.
 */
public record MandateApprovedEvent(UUID mandateId) {}
