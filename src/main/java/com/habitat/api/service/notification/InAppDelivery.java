package com.habitat.api.service.notification;

import com.habitat.api.entity.Notification;
import com.habitat.api.entity.User;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Persists the notification row that the bell drawer reads. The only
 * channel that actually materialises in the {@code notifications}
 * table — EMAIL / SMS go out-of-band and don't show up in-app.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InAppDelivery implements NotificationChannelDelivery {

    private final NotificationRepository notifications;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public DeliveryResult deliver(
            User recipient,
            NotificationCategory category,
            NotificationType type,
            String title,
            String body,
            String actionUrl,
            String actionLabel,
            UUID refId
    ) {
        Notification n = Notification.builder()
                .recipient(recipient)
                .category(category)
                .type(type)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .actionLabel(actionLabel)
                .refId(refId)
                .read(false)
                .build();
        n = notifications.save(n);
        log.debug("notification persisted: user={} type={} id={}",
                recipient.getId(), type, n.getId());
        return DeliveryResult.persisted(NotificationChannel.IN_APP, n.getId());
    }
}
