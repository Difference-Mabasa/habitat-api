package com.habitat.api.service;

import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.notification.NotificationResponse;
import com.habitat.api.dto.notification.PushResult;
import com.habitat.api.dto.notification.UnreadCountResponse;
import com.habitat.api.entity.Notification;
import com.habitat.api.entity.User;
import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.NotificationRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.notification.DeliveryResult;
import com.habitat.api.service.notification.NotificationChannelDelivery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notifications;
    @Mock SecurityUtils security;
    @Mock NotificationPreferencesService preferences;
    @Mock NotificationChannelDelivery inAppBean;
    @Mock NotificationChannelDelivery emailBean;
    @Mock NotificationChannelDelivery smsBean;

    private NotificationService service;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID NOTIF_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    void wireDeliveries() {
        when(inAppBean.channel()).thenReturn(NotificationChannel.IN_APP);
        when(emailBean.channel()).thenReturn(NotificationChannel.EMAIL);
        when(smsBean.channel()).thenReturn(NotificationChannel.SMS);
        service = new NotificationService(
                notifications, security, preferences, List.of(inAppBean, emailBean, smsBean));
    }

    void wireReadOnly() {
        // For read-path tests we don't need the delivery beans wired; passing
        // an empty list keeps Mockito strict-stubbing happy.
        service = new NotificationService(notifications, security, preferences, List.of());
    }

    @Test
    void getMyNotifications_returns_paginated_response_for_caller() {
        wireReadOnly();
        when(security.requireUserId()).thenReturn(USER_ID);
        Notification n = notifFor(userWithId(USER_ID), false);
        Page<Notification> page = new PageImpl<>(List.of(n));
        when(notifications.findByRecipientIdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<NotificationResponse> out = service.getMyNotifications(0, 20);

        assertThat(out.content()).hasSize(1);
        assertThat(out.content().get(0).title()).isEqualTo("Welcome to Habitat!");
    }

    @Test
    void getUnreadCount_returns_count_for_caller() {
        wireReadOnly();
        when(security.requireUserId()).thenReturn(USER_ID);
        when(notifications.countByRecipientIdAndReadFalse(USER_ID)).thenReturn(3L);

        UnreadCountResponse out = service.getUnreadCount();

        assertThat(out.count()).isEqualTo(3L);
    }

    @Test
    void markAsRead_flips_unread_to_read_and_stamps_readAt() {
        wireReadOnly();
        when(security.requireUserId()).thenReturn(USER_ID);
        Notification n = notifFor(userWithId(USER_ID), false);
        when(notifications.findById(NOTIF_ID)).thenReturn(Optional.of(n));

        NotificationResponse out = service.markAsRead(NOTIF_ID);

        assertThat(n.isRead()).isTrue();
        assertThat(n.getReadAt()).isNotNull();
        assertThat(out.read()).isTrue();
    }

    @Test
    void markAsRead_returns_404_for_someone_elses_notification() {
        wireReadOnly();
        when(security.requireUserId()).thenReturn(USER_ID);
        Notification n = notifFor(userWithId(OTHER_USER), false);
        when(notifications.findById(NOTIF_ID)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.markAsRead(NOTIF_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_soft_deletes_owned_notification() {
        wireReadOnly();
        when(security.requireUserId()).thenReturn(USER_ID);
        Notification n = notifFor(userWithId(USER_ID), false);
        when(notifications.findById(NOTIF_ID)).thenReturn(Optional.of(n));

        service.delete(NOTIF_ID);

        assertThat(n.getDeletedAt()).isNotNull();
    }

    // ── push routing ─────────────────────────────────────────────────────

    @Test
    void push_routes_to_every_default_channel_when_preferences_allow() {
        wireDeliveries();
        User recipient = userWithId(USER_ID);
        // WELCOME defaults to IN_APP + EMAIL. Opt-in for both.
        when(preferences.isOptedIn(USER_ID, NotificationCategory.ONBOARDING, NotificationChannel.IN_APP))
                .thenReturn(true);
        when(preferences.isOptedIn(USER_ID, NotificationCategory.ONBOARDING, NotificationChannel.EMAIL))
                .thenReturn(true);
        when(inAppBean.deliver(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(DeliveryResult.persisted(NotificationChannel.IN_APP, NOTIF_ID));
        when(emailBean.deliver(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(DeliveryResult.pendingProvider(NotificationChannel.EMAIL));

        PushResult result = service.push(
                recipient, NotificationType.WELCOME, "Welcome!", "Body", "/profile/onboarding", "Complete Profile", null);

        assertThat(result.byChannel()).containsKeys(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        assertThat(result.byChannel().get(NotificationChannel.IN_APP).status())
                .isEqualTo(DeliveryResult.Status.PERSISTED);
        assertThat(result.byChannel().get(NotificationChannel.EMAIL).status())
                .isEqualTo(DeliveryResult.Status.PENDING_PROVIDER);
        assertThat(result.inAppNotificationId()).contains(NOTIF_ID);
        verify(inAppBean).deliver(eq(recipient), eq(NotificationCategory.ONBOARDING),
                eq(NotificationType.WELCOME), any(), any(), any(), any(), any());
        verify(emailBean).deliver(eq(recipient), eq(NotificationCategory.ONBOARDING),
                eq(NotificationType.WELCOME), any(), any(), any(), any(), any());
        verify(smsBean, never()).deliver(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void push_skips_channels_user_opted_out_of() {
        wireDeliveries();
        User recipient = userWithId(USER_ID);
        // Opted out of EMAIL for ONBOARDING; IN_APP still on.
        when(preferences.isOptedIn(USER_ID, NotificationCategory.ONBOARDING, NotificationChannel.IN_APP))
                .thenReturn(true);
        when(preferences.isOptedIn(USER_ID, NotificationCategory.ONBOARDING, NotificationChannel.EMAIL))
                .thenReturn(false);
        when(inAppBean.deliver(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(DeliveryResult.persisted(NotificationChannel.IN_APP, NOTIF_ID));

        PushResult result = service.push(
                recipient, NotificationType.WELCOME, "Welcome!", "Body", null, null, null);

        assertThat(result.byChannel().get(NotificationChannel.EMAIL).status())
                .isEqualTo(DeliveryResult.Status.SKIPPED_BY_PREFERENCE);
        assertThat(result.byChannel().get(NotificationChannel.IN_APP).status())
                .isEqualTo(DeliveryResult.Status.PERSISTED);
        verify(emailBean, never()).deliver(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void push_isolates_channel_failures_from_other_channels() {
        wireDeliveries();
        User recipient = userWithId(USER_ID);
        when(preferences.isOptedIn(USER_ID, NotificationCategory.ONBOARDING, NotificationChannel.IN_APP))
                .thenReturn(true);
        when(preferences.isOptedIn(USER_ID, NotificationCategory.ONBOARDING, NotificationChannel.EMAIL))
                .thenReturn(true);
        when(inAppBean.deliver(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(DeliveryResult.persisted(NotificationChannel.IN_APP, NOTIF_ID));
        when(emailBean.deliver(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("provider down"));

        PushResult result = service.push(
                recipient, NotificationType.WELCOME, "Welcome!", "Body", null, null, null);

        assertThat(result.byChannel().get(NotificationChannel.IN_APP).status())
                .isEqualTo(DeliveryResult.Status.PERSISTED);
        assertThat(result.byChannel().get(NotificationChannel.EMAIL).status())
                .isEqualTo(DeliveryResult.Status.FAILED);
        assertThat(result.byChannel().get(NotificationChannel.EMAIL).error())
                .isEqualTo("provider down");
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static User userWithId(UUID id) {
        User u = User.builder()
                .email("x@example.co.za")
                .firstName("Sipho")
                .surname("Dlamini")
                .build();
        setId(u, id);
        return u;
    }

    private static Notification notifFor(User owner, boolean alreadyRead) {
        Notification n = Notification.builder()
                .recipient(owner)
                .category(NotificationCategory.ONBOARDING)
                .type(NotificationType.WELCOME)
                .title("Welcome to Habitat!")
                .body("Body")
                .read(alreadyRead)
                .build();
        if (alreadyRead) n.setReadAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
        setId(n, NOTIF_ID);
        return n;
    }

    private static void setId(BaseEntity e, UUID id) {
        try {
            java.lang.reflect.Field f = BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
