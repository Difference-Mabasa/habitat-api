package com.habitat.api.event;

import java.util.UUID;

/**
 * Published when an online landlord opens a structured change request
 * via {@link com.habitat.api.service.MandateService#requestChanges}.
 * The listener pushes MANDATE_CHANGES_REQUESTED to the agent so they
 * can read the items + comment and revise (resubmit) or withdraw.
 */
public record MandateChangesRequestedEvent(UUID mandateId, UUID changeRequestId) {}
