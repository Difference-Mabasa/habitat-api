package com.habitat.api.service.notification;

import com.habitat.api.entity.User;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Email-channel stub. A future slice will wire a real provider
 * (Resend, AWS SES, …) behind this same interface — the router doesn't
 * change. For now we log enough to verify the routing decisions reach
 * the email pipeline without actually sending mail.
 */
@Component
@Slf4j
public class EmailDelivery implements NotificationChannelDelivery {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
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
        // Intentionally no PII in the log — email + body are not logged.
        log.info("email delivery stub: user={} type={} category={} actionUrl={}",
                recipient.getId(), type, category, actionUrl);
        return DeliveryResult.pendingProvider(NotificationChannel.EMAIL);
    }
}
