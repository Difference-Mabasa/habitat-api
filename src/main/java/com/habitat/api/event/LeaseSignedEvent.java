package com.habitat.api.event;

import java.util.UUID;

/**
 * Published by {@link com.habitat.api.service.LeaseService#sign} the moment
 * the second signature lands and the lease flips to {@code SIGNED}.
 *
 * <p>Listeners run via {@code @TransactionalEventListener(phase = AFTER_COMMIT)
 * + @Transactional(REQUIRES_NEW)} so the move-in side effects (application
 * → {@code COMPLETED}, unit → {@code OCCUPIED}, party notifications)
 * only fire once the signature has actually committed — a rolled-back
 * sign transaction leaves no stranded side effects.
 *
 * <p>Carries only the {@code leaseId}: the listener re-loads the lease
 * in its own transaction so it works against fresh state. Mirrors
 * {@link UserRegisteredEvent}'s shape.
 *
 * <p>This is the first instance of the event-driven pattern in the
 * application-lifecycle services (per habitat-api/TECH_DEBT.md ARCH-03)
 * — earlier slices used direct service calls.
 */
public record LeaseSignedEvent(UUID leaseId) {}
