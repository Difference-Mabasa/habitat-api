package com.habitat.api.event;

import java.util.UUID;

/**
 * Published when the agent resubmits a revised mandate via
 * {@link com.habitat.api.service.MandateService#resubmit}.
 * The listener pushes MANDATE_RESUBMITTED to the landlord so they
 * know it's back in their court for round-2+ sign-off.
 */
public record MandateResubmittedEvent(UUID mandateId) {}
