package com.habitat.api.event;

import java.util.UUID;

/**
 * Published by {@link com.habitat.api.service.ApplicationService#review}
 * when the landlord/agent parks an application at ON_HOLD — usually
 * to request fresher docs / more info, the ask carried on the
 * decision note. Listener pushes to the tenant.
 */
public record ApplicationOnHoldEvent(UUID applicationId) {}
