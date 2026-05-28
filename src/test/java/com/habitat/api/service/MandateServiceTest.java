package com.habitat.api.service;

import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.mandate.ApproveMandateRequest;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.dto.mandate.RejectMandateRequest;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;
import com.habitat.api.event.MandateActiveEvent;
import com.habitat.api.event.MandateApprovedEvent;
import com.habitat.api.event.MandateRejectedEvent;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.repository.MandateRepository;
import com.habitat.api.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
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

/**
 * Focused coverage for the landlord-side approve / reject paths added
 * with the property-listing comms slice. The existing happy-path
 * issue/uploadSigned flows are covered indirectly by the wizard +
 * integration tests; this test pins the authz + state-transition +
 * event-publishing rules introduced here.
 */
@ExtendWith(MockitoExtension.class)
class MandateServiceTest {

    @Mock MandateRepository mandates;
    @Mock com.habitat.api.repository.PropertyRepository properties;
    @Mock com.habitat.api.repository.UserRepository users;
    @Mock PropertyService propertyService;
    @Mock LandlordService landlordService;
    @Mock MandatePdfService mandatePdf;
    @Mock com.habitat.api.storage.StorageService storage;
    @Mock SecurityUtils security;
    @Mock ApplicationEventPublisher events;
    @InjectMocks MandateService service;

    private static final UUID PROP_ID    = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID MANDATE_ID = UUID.fromString("ffffffff-eeee-dddd-cccc-bbbbbbbbbbbb");
    private static final UUID OWNER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID AGENT_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final ApproveMandateRequest VALID_APPROVE = new ApproveMandateRequest("Naledi Owner");
    private static final RejectMandateRequest VALID_REJECT =
            new RejectMandateRequest("Fee too high for tenant-find market.");

    // ── approveByLandlord ────────────────────────────────────────────

    @Test
    void approve_flips_status_to_active_and_stores_signed_fields_and_publishes_both_events() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.approveByLandlord(PROP_ID, VALID_APPROVE);

        assertThat(m.getStatus()).isEqualTo(MandateStatus.ACTIVE);
        assertThat(m.getSignedName()).isEqualTo("Naledi Owner");
        assertThat(m.getSignedAt()).isNotNull();
        verify(events).publishEvent(any(MandateApprovedEvent.class));
        verify(events).publishEvent(any(MandateActiveEvent.class));
    }

    @Test
    void approve_with_mismatched_typed_name_400s_and_does_not_mutate() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        assertThatThrownBy(() ->
                service.approveByLandlord(PROP_ID, new ApproveMandateRequest("Wrong Name")))
                .isInstanceOf(BadRequestException.class);

        assertThat(m.getStatus()).isEqualTo(MandateStatus.PENDING_LANDLORD_APPROVAL);
        assertThat(m.getSignedName()).isNull();
        assertThat(m.getSignedAt()).isNull();
        verify(events, never()).publishEvent(any());
    }

    @Test
    void approve_normalises_case_and_internal_whitespace_when_matching() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        // "  naledi   owner " → normalised matches "Naledi Owner". Stored
        // verbatim (with surrounding whitespace trimmed) so the PDF
        // shows the landlord's typed casing, not the registered form.
        service.approveByLandlord(PROP_ID, new ApproveMandateRequest("  naledi   owner "));

        assertThat(m.getStatus()).isEqualTo(MandateStatus.ACTIVE);
        assertThat(m.getSignedName()).isEqualTo("naledi   owner");
    }

    @Test
    void approve_forbidden_when_caller_is_not_the_landlord_user() {
        User otherUser = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(otherUser), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.approveByLandlord(PROP_ID, VALID_APPROVE))
                .isInstanceOf(ForbiddenException.class);
        verify(events, never()).publishEvent(any());
    }

    @Test
    void approve_forbidden_when_landlord_is_offline() {
        // No User behind an OFFLINE landlord — no one can approve.
        Landlord offline = Landlord.builder()
                .type(LandlordType.OFFLINE)
                .firstName("Naledi").lastName("Owner")
                .build();
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                offline, MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        assertThatThrownBy(() -> service.approveByLandlord(PROP_ID, VALID_APPROVE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approve_conflict_when_status_isnt_pending_landlord_approval() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.ACTIVE);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        assertThatThrownBy(() -> service.approveByLandlord(PROP_ID, VALID_APPROVE))
                .isInstanceOf(ConflictException.class);
    }

    // ── rejectByLandlord ─────────────────────────────────────────────

    @Test
    void reject_flips_status_and_stores_reason_and_rejected_at_and_publishes_event() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.rejectByLandlord(PROP_ID, VALID_REJECT);

        assertThat(m.getStatus()).isEqualTo(MandateStatus.REJECTED);
        assertThat(m.getRejectionReason()).isEqualTo("Fee too high for tenant-find market.");
        assertThat(m.getRejectedAt()).isNotNull();
        verify(events).publishEvent(any(MandateRejectedEvent.class));
    }

    @Test
    void reject_trims_reason_surrounding_whitespace_but_preserves_internal() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.rejectByLandlord(PROP_ID, new RejectMandateRequest("  Too\n  steep.  "));

        assertThat(m.getRejectionReason()).isEqualTo("Too\n  steep.");
    }

    @Test
    void reject_forbidden_when_caller_is_not_the_landlord_user() {
        User otherUser = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(otherUser), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findFirstByProperty_IdOrderByCreatedAtDesc(PROP_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.rejectByLandlord(PROP_ID, VALID_REJECT))
                .isInstanceOf(ForbiddenException.class);
        verify(events, never()).publishEvent(any());
    }

    // ── listAwaitingMyApproval (paginated) ───────────────────────────

    @Test
    void list_returns_paged_mandates_for_landlord() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(security.requireUserId()).thenReturn(OWNER_ID);
        Page<Mandate> page = new PageImpl<>(List.of(m), PageRequest.of(0, 20), 1);
        when(mandates.findByStatusAndProperty_Landlord_User_IdOrderByCreatedAtDesc(
                eq(MandateStatus.PENDING_LANDLORD_APPROVAL), eq(OWNER_ID), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<MandateResponse> result = service.listAwaitingMyApproval(0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).propertyId()).isEqualTo(PROP_ID);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void list_clamps_size_above_100_to_100() {
        when(security.requireUserId()).thenReturn(OWNER_ID);
        when(mandates.findByStatusAndProperty_Landlord_User_IdOrderByCreatedAtDesc(
                any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        service.listAwaitingMyApproval(0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(mandates).findByStatusAndProperty_Landlord_User_IdOrderByCreatedAtDesc(
                eq(MandateStatus.PENDING_LANDLORD_APPROVAL), eq(OWNER_ID), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void list_clamps_negative_page_to_zero_and_zero_size_to_one() {
        when(security.requireUserId()).thenReturn(OWNER_ID);
        when(mandates.findByStatusAndProperty_Landlord_User_IdOrderByCreatedAtDesc(
                any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        service.listAwaitingMyApproval(-5, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(mandates).findByStatusAndProperty_Landlord_User_IdOrderByCreatedAtDesc(
                any(), any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static Landlord onlineLandlord(User u) {
        return Landlord.builder().type(LandlordType.ONLINE).user(u).build();
    }

    private static Mandate mandate(User agent, Landlord landlord, MandateStatus status) {
        Property property = withId(Property.builder()
                .title("Sandton Villa")
                .landlord(landlord)
                .manager(agent)
                .build(), PROP_ID);
        return withId(Mandate.builder()
                .property(property)
                .agent(agent)
                .mandateType(MandateType.FULL_MANAGEMENT)
                .status(status)
                .feePercent(new BigDecimal("8.0"))
                .build(), MANDATE_ID);
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
