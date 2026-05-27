package com.habitat.api.event;

import java.util.UUID;

/**
 * Published when an online landlord rejects a mandate via
 * {@link com.habitat.api.service.MandateService#rejectByLandlord}.
 * The listener pushes MANDATE_REJECTED to the agent so they can
 * follow up out-of-band to revise terms before re-issuing.
 */
public record MandateRejectedEvent(UUID mandateId) {}
