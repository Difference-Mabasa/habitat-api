package com.habitat.api.event;

import com.habitat.api.dto.notification.PushResult;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.repository.ApplicationRepository;
import com.habitat.api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
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
class ApplicationSubmittedListenerTest {

    @Mock ApplicationRepository applications;
    @Mock NotificationService notifications;
    @InjectMocks ApplicationSubmittedListener listener;

    private static final UUID APP_ID    = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LL_ID     = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MGR_ID    = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID UNIT_ID   = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PROP_ID   = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    void pushes_manager_and_tenant_when_landlord_equals_manager() {
        // Owner-managed listing — landlord and manager are the same user.
        User owner = user(LL_ID, "Naledi", "Mokoena");
        User tenant = user(TENANT_ID, "Sipho", "Khumalo");
        Application app = applicationWith(owner, owner, tenant);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(app));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onApplicationSubmitted(new ApplicationSubmittedEvent(APP_ID));

        // Manager (= landlord) gets ONE APPLICATION_RECEIVED — no duplicate.
        verify(notifications, times(1))
                .push(eq(owner), eq(NotificationType.APPLICATION_RECEIVED),
                        any(), any(), any(), any(), eq(APP_ID));
        // Tenant gets the confirmation.
        verify(notifications)
                .push(eq(tenant), eq(NotificationType.APPLICATION_SUBMITTED_TENANT),
                        any(), any(), any(), any(), eq(APP_ID));
    }

    @Test
    void pushes_manager_landlord_and_tenant_when_distinct() {
        // Agent-managed listing — landlord ≠ manager.
        User landlord = user(LL_ID, "Naledi", "Owner");
        User manager  = user(MGR_ID, "Pieter", "Agent");
        User tenant   = user(TENANT_ID, "Sipho", "Khumalo");
        Application app = applicationWith(landlord, manager, tenant);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(app));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onApplicationSubmitted(new ApplicationSubmittedEvent(APP_ID));

        verify(notifications)
                .push(eq(manager), eq(NotificationType.APPLICATION_RECEIVED),
                        any(), any(), any(), any(), eq(APP_ID));
        verify(notifications)
                .push(eq(landlord), eq(NotificationType.APPLICATION_RECEIVED),
                        any(), any(), any(), any(), eq(APP_ID));
        verify(notifications)
                .push(eq(tenant), eq(NotificationType.APPLICATION_SUBMITTED_TENANT),
                        any(), any(), any(), any(), eq(APP_ID));
    }

    @Test
    void skips_silently_when_application_vanishes() {
        when(applications.findById(APP_ID)).thenReturn(Optional.empty());
        listener.onApplicationSubmitted(new ApplicationSubmittedEvent(APP_ID));
        verify(notifications, never()).push(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void swallows_individual_push_failures() {
        User owner = user(LL_ID, "Naledi", "Mokoena");
        User tenant = user(TENANT_ID, "Sipho", "Khumalo");
        Application app = applicationWith(owner, owner, tenant);
        when(applications.findById(APP_ID)).thenReturn(Optional.of(app));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("downstream blew up"));

        listener.onApplicationSubmitted(new ApplicationSubmittedEvent(APP_ID));
        // Still attempted both pushes (manager + tenant) despite the throws.
        verify(notifications, times(2))
                .push(any(), any(), any(), any(), any(), any(), any());
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private Application applicationWith(User landlord, User manager, User tenant) {
        Property property = Property.builder()
                .landlord(landlord)
                .manager(manager)
                .title("Olive Court")
                .build();
        setId(property, PROP_ID);
        Unit unit = Unit.builder()
                .status(UnitStatus.AVAILABLE)
                .property(property)
                .price(new BigDecimal("8500"))
                .title("Unit A")
                .build();
        setId(unit, UNIT_ID);
        Application app = Application.builder()
                .unit(unit)
                .tenant(tenant)
                .status(ApplicationStatus.SUBMITTED)
                .build();
        setId(app, APP_ID);
        return app;
    }

    private static User user(UUID id, String first, String last) {
        User u = User.builder()
                .firstName(first)
                .surname(last)
                .email((first + "@example.co.za").toLowerCase())
                .build();
        setId(u, id);
        return u;
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
