package com.habitat.api.event;

import java.util.UUID;

/**
 * Published when the agent withdraws a pending mandate via
 * {@link com.habitat.api.service.MandateService#withdraw}.
 * The listener pushes MANDATE_WITHDRAWN to the landlord so they
 * know the agent gave up rather than revise further.
 */
public record MandateWithdrawnEvent(UUID mandateId) {}
