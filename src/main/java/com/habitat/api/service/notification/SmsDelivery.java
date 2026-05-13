package com.habitat.api.service.notification;

import com.habitat.api.entity.User;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SMS-channel stub. A future slice will plug in a provider (Clickatell,
 * Vodacom Bulk SMS, …) behind this same interface.
 */
@Component
@Slf4j
public class SmsDelivery implements NotificationChannelDelivery {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
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
        log.info("sms delivery stub: user={} type={} category={}",
                recipient.getId(), type, category);
        return DeliveryResult.pendingProvider(NotificationChannel.SMS);
    }
}
