package com.habitat.api.service.notification;

import com.habitat.api.entity.User;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.enums.NotificationType;

import java.util.UUID;

/**
 * One implementation per {@link NotificationChannel}. The NotificationService
 * router collects every bean implementing this and dispatches by
 * {@link #channel()}.
 *
 * Keep implementations stateless — they're singletons and may be called
 * concurrently. State (e.g. retry queues) belongs in a separate service
 * that this delivery talks to.
 */
public interface NotificationChannelDelivery {

    NotificationChannel channel();

    /**
     * Deliver a notification of {@code type} to {@code recipient} on this
     * channel. Implementations decide whether to persist (IN_APP) or fire
     * an out-of-band send (EMAIL / SMS).
     *
     * @return the channel-specific delivery result. Never null. A
     *         FAILED status should NOT throw — the router treats each
     *         channel independently so one failure doesn't block the
     *         others.
     */
    DeliveryResult deliver(
            User recipient,
            NotificationCategory category,
            NotificationType type,
            String title,
            String body,
            String actionUrl,
            String actionLabel,
            UUID refId);
}
