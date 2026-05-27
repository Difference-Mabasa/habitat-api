package com.habitat.api.service;

import com.habitat.api.dto.viewing.RequestViewingRequest;
import com.habitat.api.dto.viewing.ReviewViewingRequest;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.entity.Viewing;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.ViewingStatus;
import com.habitat.api.event.ViewingApprovedEvent;
import com.habitat.api.event.ViewingCancelledEvent;
import com.habitat.api.event.ViewingRejectedEvent;
import com.habitat.api.event.ViewingRequestedEvent;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.repository.ViewingRepository;
import com.habitat.api.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewingServiceTest {

    @Mock ViewingRepository viewings;
    @Mock UnitRepository units;
    @Mock UserRepository users;
    @Mock PropertyService propertyService;
    @Mock SecurityUtils security;
    @Mock ApplicationEventPublisher events;
    @InjectMocks ViewingService service;

    private static final UUID TENANT_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MANAGER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OUTSIDER   = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID UNIT_ID    = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROP_ID    = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID VIEW_ID    = UUID.fromString("55555555-5555-5555-5555-555555555555");

    // ── request ──────────────────────────────────────────────────────

    @Test
    void request_persists_and_publishes_event() {
        User tenant = user(TENANT_ID);
        Unit unit = unitWith(UnitStatus.AVAILABLE);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(users.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(viewings.save(any())).thenAnswer(i -> {
            Viewing v = i.getArgument(0);
            setId(v, VIEW_ID);
            return v;
        });

        var req = new RequestViewingRequest(
                UNIT_ID, OffsetDateTime.now().plusDays(3), "Anytime after 4pm");

        service.request(req);

        verify(events).publishEvent(any(ViewingRequestedEvent.class));
    }

    @Test
    void request_rejects_unavailable_units() {
        User tenant = user(TENANT_ID);
        Unit unit = unitWith(UnitStatus.OCCUPIED);
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(users.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(units.findById(UNIT_ID)).thenReturn(Optional.of(unit));

        var req = new RequestViewingRequest(
                UNIT_ID, OffsetDateTime.now().plusDays(3), null);

        assertThatThrownBy(() -> service.request(req))
                .isInstanceOf(BadRequestException.class);
    }

    // ── approve ──────────────────────────────────────────────────────

    @Test
    void approve_transitions_to_approved_and_publishes_event() {
        Viewing v = viewingWith(ViewingStatus.REQUESTED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(security.requireUserId()).thenReturn(MANAGER_ID);

        service.approve(VIEW_ID, new ReviewViewingRequest("Garage access only"));

        assertThat(v.getStatus()).isEqualTo(ViewingStatus.APPROVED);
        assertThat(v.getDecisionNote()).isEqualTo("Garage access only");
        assertThat(v.getDecidedBy()).isEqualTo(MANAGER_ID);
        verify(events).publishEvent(any(ViewingApprovedEvent.class));
    }

    @Test
    void approve_forbidden_when_caller_cant_edit_property() {
        Viewing v = viewingWith(ViewingStatus.REQUESTED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        org.mockito.Mockito.doThrow(new ForbiddenException("nope"))
                .when(propertyService).requireCanEdit(any());

        assertThatThrownBy(() -> service.approve(VIEW_ID, null))
                .isInstanceOf(ForbiddenException.class);
        verify(events, never()).publishEvent(any());
    }

    @Test
    void approve_conflict_when_terminal_state() {
        // CANCELLED → APPROVED is an illegal transition. Idempotency
        // (APPROVED → APPROVED) is allowed by design, mirroring
        // ApplicationStateMachine.
        Viewing v = viewingWith(ViewingStatus.CANCELLED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.approve(VIEW_ID, null))
                .isInstanceOf(ConflictException.class);
    }

    // ── reject ───────────────────────────────────────────────────────

    @Test
    void reject_transitions_and_publishes_event() {
        Viewing v = viewingWith(ViewingStatus.REQUESTED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(security.requireUserId()).thenReturn(MANAGER_ID);

        service.reject(VIEW_ID, new ReviewViewingRequest("Already let"));

        assertThat(v.getStatus()).isEqualTo(ViewingStatus.REJECTED);
        verify(events).publishEvent(any(ViewingRejectedEvent.class));
    }

    // ── cancel ───────────────────────────────────────────────────────

    @Test
    void cancel_by_tenant_allowed_records_actor() {
        Viewing v = viewingWith(ViewingStatus.APPROVED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(security.requireUserId()).thenReturn(TENANT_ID);
        when(propertyService.canEdit(any())).thenReturn(false);

        service.cancel(VIEW_ID);

        assertThat(v.getStatus()).isEqualTo(ViewingStatus.CANCELLED);
        assertThat(v.getCancelledBy()).isEqualTo(TENANT_ID);
        verify(events).publishEvent(any(ViewingCancelledEvent.class));
    }

    @Test
    void cancel_by_manager_allowed() {
        Viewing v = viewingWith(ViewingStatus.APPROVED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(security.requireUserId()).thenReturn(MANAGER_ID);
        when(propertyService.canEdit(any())).thenReturn(true);

        service.cancel(VIEW_ID);

        assertThat(v.getStatus()).isEqualTo(ViewingStatus.CANCELLED);
        assertThat(v.getCancelledBy()).isEqualTo(MANAGER_ID);
    }

    @Test
    void cancel_forbidden_for_outsider() {
        Viewing v = viewingWith(ViewingStatus.APPROVED);
        when(viewings.findById(VIEW_ID)).thenReturn(Optional.of(v));
        when(security.requireUserId()).thenReturn(OUTSIDER);
        when(propertyService.canEdit(any())).thenReturn(false);

        assertThatThrownBy(() -> service.cancel(VIEW_ID))
                .isInstanceOf(ForbiddenException.class);
        verify(events, never()).publishEvent(any());
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static Unit unitWith(UnitStatus status) {
        User manager = user(MANAGER_ID);
        Property property = withId(Property.builder()
                .manager(manager)
                .title("Sandton Villa")
                .build(), PROP_ID);
        return withId(Unit.builder()
                .property(property)
                .title("Unit A")
                .status(status)
                .build(), UNIT_ID);
    }

    private static Viewing viewingWith(ViewingStatus status) {
        User tenant = user(TENANT_ID);
        Unit unit = unitWith(UnitStatus.AVAILABLE);
        return withId(Viewing.builder()
                .unit(unit)
                .tenant(tenant)
                .scheduledAt(OffsetDateTime.now().plusDays(3))
                .status(status)
                .build(), VIEW_ID);
    }

    private static User user(UUID id) {
        User u = User.builder()
                .firstName("U").surname("ser")
                .email("u@example.co.za")
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
