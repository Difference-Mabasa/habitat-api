package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.notification.NotificationResponse;
import com.habitat.api.dto.notification.PushResult;
import com.habitat.api.dto.notification.UnreadCountResponse;
import com.habitat.api.entity.Notification;
import com.habitat.api.entity.User;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.NotificationRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.notification.DeliveryResult;
import com.habitat.api.service.notification.NotificationChannelDelivery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-app notifications + multi-channel delivery routing.
 *
 * Public surface:
 *   <ul>
 *     <li>Read paths (getMyNotifications, getUnreadCount, markAsRead,
 *         markAllAsRead, delete) operate against the {@code notifications}
 *         table — only the IN_APP channel ever materialises rows here.</li>
 *     <li>Internal push(...) is the single entry point for other services
 *         and event listeners. It resolves the type's default channels
 *         against the recipient's preferences and dispatches to each
 *         channel's delivery bean. SYSTEM × IN_APP is forced-on by
 *         {@link NotificationPreferencesService}.</li>
 *   </ul>
 *
 * Cross-user access on owned writes returns 404 (not 403) — backroom's
 * deliberate choice to avoid leaking valid IDs.
 */
@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notifications;
    private final SecurityUtils security;
    private final NotificationPreferencesService preferences;
    private final Map<NotificationChannel, NotificationChannelDelivery> deliveries;

    public NotificationService(
            NotificationRepository notifications,
            SecurityUtils security,
            NotificationPreferencesService preferences,
            List<NotificationChannelDelivery> deliveryBeans
    ) {
        this.notifications = notifications;
        this.security = security;
        this.preferences = preferences;
        this.deliveries = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannelDelivery d : deliveryBeans) {
            this.deliveries.put(d.channel(), d);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(int page, int size) {
        UUID me = security.requireUserId();
        Pageable req = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> rows = notifications.findByRecipientIdOrderByCreatedAtDesc(me, req);
        return PageResponse.from(rows.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        UUID me = security.requireUserId();
        return new UnreadCountResponse(notifications.countByRecipientIdAndReadFalse(me));
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        UUID me = security.requireUserId();
        Notification n = ownedNotificationOrNotFound(id, me);
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllAsRead() {
        UUID me = security.requireUserId();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int touched = notifications.markAllAsRead(me, now);
        log.debug("marked {} notifications as read for user {}", touched, me);
        return touched;
    }

    @Transactional
    public void delete(UUID id) {
        UUID me = security.requireUserId();
        Notification n = ownedNotificationOrNotFound(id, me);
        n.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Push a notification to {@code recipient}. The {@link NotificationType}
     * declares its category + default channels; this method narrows the
     * channel set by the recipient's preferences and dispatches each
     * surviving channel to its delivery bean. Failures are isolated per
     * channel — one provider going down doesn't block the others.
     */
    @Transactional
    public PushResult push(
            User recipient,
            NotificationType type,
            String title,
            String body,
            String actionUrl,
            String actionLabel,
            UUID refId
    ) {
        Map<NotificationChannel, DeliveryResult> byChannel = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel channel : type.getDefaultChannels()) {
            if (!preferences.isOptedIn(recipient.getId(), type.getCategory(), channel)) {
                byChannel.put(channel, DeliveryResult.skipped(channel));
                continue;
            }
            NotificationChannelDelivery delivery = deliveries.get(channel);
            if (delivery == null) {
                // Defensive: a default channel with no bean wired (should
                // never happen because every Channel value has a @Component
                // implementation, but guard anyway so a misconfig doesn't
                // crash a registration).
                log.warn("no delivery bean for channel {} — skipping", channel);
                byChannel.put(channel, DeliveryResult.failed(channel, "no delivery bean"));
                continue;
            }
            try {
                byChannel.put(channel, delivery.deliver(
                        recipient, type.getCategory(), type, title, body, actionUrl, actionLabel, refId));
            } catch (RuntimeException ex) {
                log.warn("channel {} failed for user {}: {}", channel, recipient.getId(), ex.getMessage(), ex);
                byChannel.put(channel, DeliveryResult.failed(channel, ex.getMessage()));
            }
        }
        log.info("notification pushed: user={} type={} channels={}",
                recipient.getId(), type, byChannel.keySet());
        return new PushResult(byChannel);
    }

    private Notification ownedNotificationOrNotFound(UUID id, UUID me) {
        return notifications.findById(id)
                .filter(n -> n.getRecipient() != null && me.equals(n.getRecipient().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.NOTIFICATION_NOT_FOUND));
    }
}
