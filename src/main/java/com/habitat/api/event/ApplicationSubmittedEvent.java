package com.habitat.api.event;

import java.util.UUID;

/**
 * Published by {@link com.habitat.api.service.ApplicationService#create}
 * the moment a new application row is persisted.
 *
 * <p>Listeners fan out the appropriate notifications via
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT) +
 * @Transactional(REQUIRES_NEW)} so they only fire if the create
 * transaction actually committed — no stranded "application received"
 * notification if the original transaction rolls back.
 *
 * <p>Mirrors {@link UserRegisteredEvent}'s shape: carries only the id,
 * the listener re-loads in its own transaction.
 */
public record ApplicationSubmittedEvent(UUID applicationId) {}
