package com.habitat.api.event;

import java.util.UUID;

/**
 * Published by {@link com.habitat.api.service.ApplicationService#review}
 * when the landlord/agent transitions the application to REJECTED.
 * Listener fans the rejection acknowledgement to the tenant with the
 * reviewer's decision note when set.
 */
public record ApplicationRejectedEvent(UUID applicationId) {}
