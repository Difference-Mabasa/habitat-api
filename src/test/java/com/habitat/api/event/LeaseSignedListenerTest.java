package com.habitat.api.event;

import com.habitat.api.dto.notification.PushResult;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.Lease;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.LeaseStatus;
import com.habitat.api.enums.LeaseTemplate;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.repository.LeaseRepository;
import com.habitat.api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseSignedListenerTest {

    @Mock LeaseRepository leases;
    @Mock NotificationService notifications;
    @InjectMocks LeaseSignedListener listener;

    private static final UUID LEASE_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID APP_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNIT_ID   = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROP_ID   = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID LL_ID     = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    void onLeaseSigned_flips_application_COMPLETED_and_unit_OCCUPIED() {
        Lease lease = signedLease();
        when(leases.findById(LEASE_ID)).thenReturn(Optional.of(lease));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onLeaseSigned(new LeaseSignedEvent(LEASE_ID));

        assertThat(lease.getApplication().getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(lease.getUnit().getStatus()).isEqualTo(UnitStatus.OCCUPIED);
    }

    @Test
    void onLeaseSigned_pushes_tenant_and_landlord_notifications() {
        Lease lease = signedLease();
        when(leases.findById(LEASE_ID)).thenReturn(Optional.of(lease));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onLeaseSigned(new LeaseSignedEvent(LEASE_ID));

        verify(notifications).push(
                eq(lease.getTenant()),
                eq(NotificationType.MOVE_IN_CONFIRMED_TENANT),
                any(), any(), eq("/move-in"), any(), eq(LEASE_ID));
        verify(notifications).push(
                eq(lease.getLandlord()),
                eq(NotificationType.LEASE_SIGNED_LANDLORD),
                any(), any(), eq("/lease?id=" + LEASE_ID), any(), eq(LEASE_ID));
    }

    @Test
    void onLeaseSigned_skips_when_lease_vanishes() {
        when(leases.findById(LEASE_ID)).thenReturn(Optional.empty());

        listener.onLeaseSigned(new LeaseSignedEvent(LEASE_ID));

        verify(notifications, never()).push(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onLeaseSigned_is_idempotent_on_already_completed_application() {
        Lease lease = signedLease();
        lease.getApplication().setStatus(ApplicationStatus.COMPLETED);
        lease.getUnit().setStatus(UnitStatus.OCCUPIED);
        when(leases.findById(LEASE_ID)).thenReturn(Optional.of(lease));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onLeaseSigned(new LeaseSignedEvent(LEASE_ID));

        assertThat(lease.getApplication().getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(lease.getUnit().getStatus()).isEqualTo(UnitStatus.OCCUPIED);
        // Notifications still fire — backfill is fine; tenants forgive a duplicate ping.
        verify(notifications).push(any(), eq(NotificationType.MOVE_IN_CONFIRMED_TENANT),
                any(), any(), any(), any(), any());
    }

    @Test
    void onLeaseSigned_swallows_notification_failures_after_status_flips() {
        Lease lease = signedLease();
        when(leases.findById(LEASE_ID)).thenReturn(Optional.of(lease));
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("downstream blew up"));

        listener.onLeaseSigned(new LeaseSignedEvent(LEASE_ID));

        // Status flips still committed even though notifications threw.
        assertThat(lease.getApplication().getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(lease.getUnit().getStatus()).isEqualTo(UnitStatus.OCCUPIED);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Lease signedLease() {
        User tenant = User.builder()
                .firstName("Sipho").surname("Khumalo").email("s@example.co.za").build();
        setId(tenant, TENANT_ID);
        User landlord = User.builder()
                .firstName("Naledi").surname("Mokoena").email("n@example.co.za").build();
        setId(landlord, LL_ID);
        Property property = Property.builder().manager(landlord).title("Olive Court").build();
        setId(property, PROP_ID);
        Unit unit = Unit.builder()
                .status(UnitStatus.AVAILABLE)
                .property(property)
                .price(new BigDecimal("8500"))
                .title("Unit A")
                .build();
        setId(unit, UNIT_ID);
        Application app = Application.builder()
                .unit(unit).tenant(tenant)
                .status(ApplicationStatus.LEASE_PENDING_SIGNATURES)
                .moveInDate(LocalDate.parse("2026-09-01"))
                .build();
        setId(app, APP_ID);
        Lease lease = Lease.builder()
                .application(app)
                .tenant(tenant).landlord(landlord)
                .unit(unit).property(property)
                .template(LeaseTemplate.RHA_STANDARD)
                .monthlyRent(new BigDecimal("8500"))
                .deposit(new BigDecimal("8500"))
                .termMonths(12)
                .startDate(LocalDate.parse("2026-09-01"))
                .status(LeaseStatus.SIGNED)
                .leaseRef("HB-LSE-TEST")
                .build();
        setId(lease, LEASE_ID);
        return lease;
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
