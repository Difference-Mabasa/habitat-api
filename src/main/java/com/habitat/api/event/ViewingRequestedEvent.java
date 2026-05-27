package com.habitat.api.event;

import java.util.UUID;

/**
 * Published when a tenant books a viewing slot via
 * {@link com.habitat.api.service.ViewingService#request}.
 * Listener pushes VIEWING_REQUESTED to the unit's property manager
 * (and the online owner-User if distinct).
 */
public record ViewingRequestedEvent(UUID viewingId) {}
