package com.habitat.api.service.notification;

import com.habitat.api.enums.NotificationChannel;

import java.util.UUID;

/**
 * One channel's outcome of a push.
 *
 * <ul>
 *   <li>{@link Status#PERSISTED} — IN_APP only. A notifications row was
 *       written; {@code notificationId} carries the new row's id.</li>
 *   <li>{@link Status#PENDING_PROVIDER} — EMAIL / SMS handed off (or
 *       stubbed). Real send happens asynchronously by the provider.</li>
 *   <li>{@link Status#SKIPPED_BY_PREFERENCE} — set by the router, not by
 *       a delivery, when the user opted out of this category × channel.
 *       Returned in the {@link com.habitat.api.dto.notification.PushResult}
 *       so callers can confirm what actually happened.</li>
 *   <li>{@link Status#FAILED} — the delivery attempted and reported a
 *       provider error. Carries a non-null {@code error} string.</li>
 * </ul>
 */
public record DeliveryResult(
        NotificationChannel channel,
        Status status,
        UUID notificationId,
        String error
) {
    public enum Status {
        PERSISTED,
        PENDING_PROVIDER,
        SKIPPED_BY_PREFERENCE,
        FAILED
    }

    public static DeliveryResult persisted(NotificationChannel channel, UUID notificationId) {
        return new DeliveryResult(channel, Status.PERSISTED, notificationId, null);
    }

    public static DeliveryResult pendingProvider(NotificationChannel channel) {
        return new DeliveryResult(channel, Status.PENDING_PROVIDER, null, null);
    }

    public static DeliveryResult skipped(NotificationChannel channel) {
        return new DeliveryResult(channel, Status.SKIPPED_BY_PREFERENCE, null, null);
    }

    public static DeliveryResult failed(NotificationChannel channel, String error) {
        return new DeliveryResult(channel, Status.FAILED, null, error);
    }
}
