package com.habitat.api.event;

import java.util.UUID;

/**
 * Fired whenever a mandate transitions into the ACTIVE terminal state
 * — covers both the online approval path
 * ({@link com.habitat.api.service.MandateService#approveByLandlord})
 * and the offline path
 * ({@link com.habitat.api.service.MandateService#uploadSigned} when
 * the agent is already attested). The listener pushes the
 * MANDATE_ACTIVE acknowledgement to the agent and (when ONLINE) to
 * the landlord-User.
 */
public record MandateActiveEvent(UUID mandateId) {}
