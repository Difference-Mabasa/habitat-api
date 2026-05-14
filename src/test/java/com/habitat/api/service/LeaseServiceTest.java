package com.habitat.api.service;

import com.habitat.api.dto.lease.DeclineLeaseRequest;
import com.habitat.api.dto.lease.SignLeaseRequest;
import com.habitat.api.event.LeaseSignedEvent;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.Lease;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.LeaseStatus;
import com.habitat.api.enums.LeaseTemplate;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.LeaseRepository;
import com.habitat.api.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseServiceTest {

    @Mock LeaseRepository leases;
    @Mock ApplicationEventPublisher events;
    @Mock SecurityUtils security;
    @InjectMocks LeaseService service;

    private static final UUID TENANT_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LANDLORD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNIT_ID     = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROP_ID     = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID APP_ID      = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID LEASE_ID    = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    void issueForPaidApplication_creates_lease_and_advances_application() {
        Application app = applicationWith(ApplicationStatus.DEPOSIT_PAID, new BigDecimal("9000"));
        when(leases.findByApplication_Id(app.getId())).thenReturn(Optional.empty());
        when(leases.save(any(Lease.class))).thenAnswer(inv -> inv.getArgument(0));

        Lease out = service.issueForPaidApplication(app);

        ArgumentCaptor<Lease> captor = ArgumentCaptor.forClass(Lease.class);
        verify(leases).save(captor.capture());
        Lease saved = captor.getValue();
        assertThat(saved.getMonthlyRent()).isEqualByComparingTo("9000");
        assertThat(saved.getDeposit()).isEqualByComparingTo("9000");
        assertThat(saved.getTermMonths()).isEqualTo(12);
        assertThat(saved.getStatus()).isEqualTo(LeaseStatus.PENDING_SIGNATURES);
        assertThat(saved.getTemplate()).isEqualTo(LeaseTemplate.RHA_STANDARD);
        assertThat(saved.getLeaseRef()).startsWith("HB-LSE-");
        // V22: direct party identity snapshotted at issuance.
        assertThat(saved.getTenant().getId()).isEqualTo(TENANT_ID);
        assertThat(saved.getLandlord().getId()).isEqualTo(LANDLORD_ID);
        assertThat(saved.getUnit().getId()).isEqualTo(UNIT_ID);
        assertThat(saved.getProperty().getId()).isEqualTo(PROP_ID);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.LEASE_PENDING_SIGNATURES);
        assertThat(out).isSameAs(saved);
    }

    @Test
    void issueForPaidApplication_is_idempotent() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease existing = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        when(leases.findByApplication_Id(app.getId())).thenReturn(Optional.of(existing));

        Lease out = service.issueForPaidApplication(app);

        assertThat(out).isSameAs(existing);
        verify(leases, never()).save(any(Lease.class));
    }

    @Test
    void listForTenant_returns_caller_leases() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findByTenant_IdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(java.util.List.of(l));

        var out = service.listForTenant();

        assertThat(out).hasSize(1);
        assertThat(out.get(0).tenant().id()).isEqualTo(TENANT_ID);
    }

    @Test
    void getById_visible_to_tenant() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        var out = service.getById(l.getId());

        assertThat(out.id()).isEqualTo(l.getId());
    }

    @Test
    void getById_visible_to_landlord() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        when(security.requireUserId()).thenReturn(LANDLORD_ID);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        var out = service.getById(l.getId());

        assertThat(out.id()).isEqualTo(l.getId());
    }

    @Test
    void getById_forbidden_for_strangers() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        UUID stranger = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(security.requireUserId()).thenReturn(stranger);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> service.getById(l.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_throws_when_missing() {
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findById(LEASE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(LEASE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sign_records_tenant_signature() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        var out = service.sign(l.getId(), new SignLeaseRequest("123456"));

        assertThat(l.getTenantSignedAt()).isNotNull();
        assertThat(l.getLandlordSignedAt()).isNull();
        assertThat(l.getStatus()).isEqualTo(LeaseStatus.PENDING_SIGNATURES);
        assertThat(out.tenantSignedAt()).isNotNull();
    }

    @Test
    void sign_completes_to_SIGNED_when_both_parties_have_signed() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        l.setLandlordSignedAt(java.time.OffsetDateTime.now());
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        service.sign(l.getId(), new SignLeaseRequest("123456"));

        assertThat(l.getStatus()).isEqualTo(LeaseStatus.SIGNED);
        verify(events).publishEvent(new LeaseSignedEvent(l.getId()));
    }

    @Test
    void sign_does_NOT_publish_event_on_first_signature() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        service.sign(l.getId(), new SignLeaseRequest("123456"));

        assertThat(l.getStatus()).isEqualTo(LeaseStatus.PENDING_SIGNATURES);
        verify(events, never()).publishEvent(any(LeaseSignedEvent.class));
    }

    @Test
    void sign_rejects_double_signature_from_same_party() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        l.setTenantSignedAt(java.time.OffsetDateTime.now());
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> service.sign(l.getId(), null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sign_forbidden_for_strangers() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        UUID stranger = UUID.fromString("88888888-8888-8888-8888-888888888888");
        when(security.requireUserId()).thenReturn(stranger);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> service.sign(l.getId(), null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void sign_rejects_lease_not_in_signing_state() {
        Application app = applicationWith(ApplicationStatus.COMPLETED, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.SIGNED, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> service.sign(l.getId(), null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void decline_marks_lease_DECLINED_and_records_reason() {
        Application app = applicationWith(ApplicationStatus.LEASE_PENDING_SIGNATURES, new BigDecimal("9000"));
        Lease l = leaseWith(LeaseStatus.PENDING_SIGNATURES, app);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(leases.findById(l.getId())).thenReturn(Optional.of(l));

        service.decline(l.getId(), new DeclineLeaseRequest("Found another place"));

        assertThat(l.getStatus()).isEqualTo(LeaseStatus.DECLINED);
        assertThat(l.getDeclineReason()).isEqualTo("Found another place");
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Application applicationWith(ApplicationStatus status, BigDecimal price) {
        User manager = User.builder()
                .firstName("L").surname("Landlord").email("l@example.co.za").build();
        setId(manager, LANDLORD_ID);
        Property property = Property.builder().manager(manager).build();
        setId(property, PROP_ID);
        Unit unit = Unit.builder().status(UnitStatus.AVAILABLE).property(property).price(price).build();
        setId(unit, UNIT_ID);
        User tenant = User.builder()
                .firstName("T").surname("Tenant").email("t@example.co.za").build();
        setId(tenant, TENANT_ID);
        Application app = Application.builder()
                .unit(unit).tenant(tenant).status(status)
                .moveInDate(LocalDate.parse("2026-08-01"))
                .build();
        setId(app, APP_ID);
        return app;
    }

    private Lease leaseWith(LeaseStatus status, Application app) {
        var property = app.getUnit().getProperty();
        Lease l = Lease.builder()
                .application(app)
                .tenant(app.getTenant())
                .landlord(property.getManager())
                .unit(app.getUnit())
                .property(property)
                .template(LeaseTemplate.RHA_STANDARD)
                .monthlyRent(app.getUnit().getPrice())
                .deposit(app.getUnit().getPrice())
                .termMonths(12)
                .startDate(LocalDate.parse("2026-08-01"))
                .status(status)
                .leaseRef("HB-LSE-TESTREF")
                .build();
        setId(l, LEASE_ID);
        return l;
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
