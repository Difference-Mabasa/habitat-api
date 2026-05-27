package com.habitat.api.event;

import java.util.UUID;

/**
 * Published by {@link com.habitat.api.service.PropertyService#publish}
 * once a property transitions DRAFT|UNLISTED → LISTED. The listener
 * pushes the publish notification to the manager (always) and to the
 * owner-User when ONLINE and distinct from the manager
 * (agent-managed listings).
 *
 * <p>AFTER_COMMIT + REQUIRES_NEW pattern — no stranded "listing is
 * live" notification when the publish transaction rolls back.
 */
public record PropertyPublishedEvent(UUID propertyId) {}
