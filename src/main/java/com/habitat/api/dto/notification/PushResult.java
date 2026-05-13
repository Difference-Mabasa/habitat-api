package com.habitat.api.dto.notification;

import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.service.notification.DeliveryResult;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Result of a NotificationService.push call. Lets callers verify what
 * actually happened — which channels delivered, which were skipped by
 * preferences, the id of any persisted in-app row.
 *
 * Mostly useful in tests and as a return-trail in logs. Not exposed
 * over the API; callers of push() are internal services and event
 * listeners.
 */
public record PushResult(Map<NotificationChannel, DeliveryResult> byChannel) {

    public Optional<UUID> inAppNotificationId() {
        DeliveryResult r = byChannel.get(NotificationChannel.IN_APP);
        if (r == null || r.notificationId() == null) return Optional.empty();
        return Optional.of(r.notificationId());
    }
}
