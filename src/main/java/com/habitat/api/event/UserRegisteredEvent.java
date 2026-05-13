package com.habitat.api.event;

import com.habitat.api.entity.User;

import java.util.UUID;

/**
 * Published by AuthService.register the moment a new user row is persisted.
 *
 * Listeners that need to react to first-time registration (welcome
 * notification, welcome email later, analytics ping) subscribe to this
 * via {@code @TransactionalEventListener(phase = AFTER_COMMIT)} so the
 * side effect only runs once the user row has actually committed —
 * avoids a stranded notification if the auth transaction rolls back.
 *
 * Improves on backroom's pattern, which inlines the welcome push inside
 * AuthService.register and so couples authentication to the notification
 * service. Decoupling lets us:
 *   - test AuthService without a NotificationService mock
 *   - add a future EmailService listener without re-touching AuthService
 *   - keep the welcome-notification path off the auth transaction's hot
 *     path
 */
public record UserRegisteredEvent(UUID userId, User user) {}
