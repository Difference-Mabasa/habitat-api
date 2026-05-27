package com.habitat.api.event;

import com.habitat.api.dto.notification.PushResult;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.entity.Viewing;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.enums.ViewingStatus;
import com.habitat.api.repository.ViewingRepository;
import com.habitat.api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewingLifecycleListenersTest {

    @Mock ViewingRepository viewings;
    @Mock NotificationService notifications;

    @InjectMocks ViewingRequestedListener requestedListener;
    @InjectMocks ViewingApprovedListener approvedListener;
    @InjectMocks ViewingRejectedListener rejectedListener;
    @InjectMocks ViewingCancelledListener cancelledListener;

    private static final UUID VIEW_ID    = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID TENANT_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MANAGER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNIT_ID    = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROP_ID    = UUID.fromString("44444444-4444-4444-4444-444444444444");

    // ── ViewingRequestedListener ─────────────────────────────────────

    @Test
    void requested_pushes_to_manager_with_viewings_cta() {
        User tenant = user(TENANT_ID, "Sipho", "Khumalo");
        User manager = user(MANAGER_ID, "Naledi", "Mokoena");
        Viewing v = viewing(tenant, manager, ViewingStatus.REQUESTED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        requestedListener.onViewingRequested(new ViewingRequestedEvent(VIEW_ID));

        verify(notifications).push(
                eq(manager),
                eq(NotificationType.VIEWING_REQUESTED),
                any(), any(),
                eq("/viewings"),
                any(),
                eq(VIEW_ID));
    }

    @Test
    void requested_skips_when_viewing_vanished() {
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.empty());
        requestedListener.onViewingRequested(new ViewingRequestedEvent(VIEW_ID));
        verify(notifications, never()).push(any(), any(), any(), any(), any(), any(), any());
    }

    // ── ViewingApprovedListener ──────────────────────────────────────

    @Test
    void approved_pushes_tenant_with_my_viewings_cta() {
        User tenant = user(TENANT_ID, "Sipho", "Khumalo");
        User manager = user(MANAGER_ID, "Naledi", "Mokoena");
        Viewing v = viewing(tenant, manager, ViewingStatus.APPROVED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        approvedListener.onViewingApproved(new ViewingApprovedEvent(VIEW_ID));

        verify(notifications).push(
                eq(tenant),
                eq(NotificationType.VIEWING_APPROVED),
                any(), any(),
                eq("/my-viewings"),
                any(),
                eq(VIEW_ID));
    }

    // ── ViewingRejectedListener ──────────────────────────────────────

    @Test
    void rejected_pushes_tenant() {
        User tenant = user(TENANT_ID, "Sipho", "Khumalo");
        User manager = user(MANAGER_ID, "Naledi", "Mokoena");
        Viewing v = viewing(tenant, manager, ViewingStatus.REJECTED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        rejectedListener.onViewingRejected(new ViewingRejectedEvent(VIEW_ID));

        verify(notifications).push(
                eq(tenant),
                eq(NotificationType.VIEWING_REJECTED),
                any(), any(),
                eq("/my-viewings"),
                any(),
                eq(VIEW_ID));
    }

    // ── ViewingCancelledListener ─────────────────────────────────────

    @Test
    void cancelled_by_tenant_notifies_manager_only() {
        User tenant = user(TENANT_ID, "Sipho", "Khumalo");
        User manager = user(MANAGER_ID, "Naledi", "Mokoena");
        Viewing v = viewing(tenant, manager, ViewingStatus.CANCELLED);
        v.setCancelledBy(TENANT_ID);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        cancelledListener.onViewingCancelled(new ViewingCancelledEvent(VIEW_ID));

        verify(notifications).push(
                eq(manager),
                eq(NotificationType.VIEWING_CANCELLED),
                any(), any(),
                eq("/viewings"),
                any(),
                eq(VIEW_ID));
        verify(notifications, times(1)).push(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelled_by_manager_notifies_tenant_only() {
        User tenant = user(TENANT_ID, "Sipho", "Khumalo");
        User manager = user(MANAGER_ID, "Naledi", "Mokoena");
        Viewing v = viewing(tenant, manager, ViewingStatus.CANCELLED);
        v.setCancelledBy(MANAGER_ID);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        cancelledListener.onViewingCancelled(new ViewingCancelledEvent(VIEW_ID));

        verify(notifications).push(
                eq(tenant),
                eq(NotificationType.VIEWING_CANCELLED),
                any(), any(),
                eq("/my-viewings"),
                any(),
                eq(VIEW_ID));
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static Viewing viewing(User tenant, User manager, ViewingStatus status) {
        Property property = withId(Property.builder()
                .manager(manager)
                .title("Sandton Villa")
                .build(), PROP_ID);
        Unit unit = withId(Unit.builder()
                .property(property).title("Unit A")
                .build(), UNIT_ID);
        return withId(Viewing.builder()
                .unit(unit).tenant(tenant)
                .scheduledAt(OffsetDateTime.parse("2026-06-15T14:30:00Z"))
                .status(status)
                .build(), VIEW_ID);
    }

    private static User user(UUID id, String first, String last) {
        User u = User.builder()
                .firstName(first).surname(last)
                .email((first + "@example.co.za").toLowerCase())
                .build();
        setId(u, id);
        return u;
    }

    private static <T> T withId(T entity, UUID id) {
        setId(entity, id);
        return entity;
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field f = entity.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
