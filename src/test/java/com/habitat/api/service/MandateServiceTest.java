package com.habitat.api.service;

import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.mandate.ApproveMandateRequest;
import com.habitat.api.dto.mandate.ChangeItemRequest;
import com.habitat.api.dto.mandate.MandateHistoryResponse;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.dto.mandate.RejectMandateRequest;
import com.habitat.api.dto.mandate.RequestChangesRequest;
import com.habitat.api.dto.mandate.ResubmitMandateRequest;
import com.habitat.api.dto.mandate.WithdrawMandateRequest;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.MandateChangeRequest;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ChangeRequestField;
import com.habitat.api.enums.ChangeRequestStatus;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;
import com.habitat.api.event.MandateActiveEvent;
import com.habitat.api.event.MandateApprovedEvent;
import com.habitat.api.event.MandateChangesRequestedEvent;
import com.habitat.api.event.MandateRejectedEvent;
import com.habitat.api.event.MandateResubmittedEvent;
import com.habitat.api.event.MandateWithdrawnEvent;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.repository.MandateChangeRequestRepository;
import com.habitat.api.repository.MandateRepository;
import com.habitat.api.repository.UserRepository;
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
    @Mock MandateChangeRequestRepository changeRequests;
    @Mock com.habitat.api.repository.PropertyRepository properties;
    @Mock UserRepository users;
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
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.approveByLandlord(MANDATE_ID, VALID_APPROVE);

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
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        assertThatThrownBy(() ->
                service.approveByLandlord(MANDATE_ID, new ApproveMandateRequest("Wrong Name")))
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
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        // "  naledi   owner " → normalised matches "Naledi Owner". Stored
        // verbatim (with surrounding whitespace trimmed) so the PDF
        // shows the landlord's typed casing, not the registered form.
        service.approveByLandlord(MANDATE_ID, new ApproveMandateRequest("  naledi   owner "));

        assertThat(m.getStatus()).isEqualTo(MandateStatus.ACTIVE);
        assertThat(m.getSignedName()).isEqualTo("naledi   owner");
    }

    @Test
    void approve_forbidden_when_caller_is_not_the_landlord_user() {
        User otherUser = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(otherUser), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.approveByLandlord(MANDATE_ID, VALID_APPROVE))
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
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        assertThatThrownBy(() -> service.approveByLandlord(MANDATE_ID, VALID_APPROVE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approve_conflict_when_status_isnt_pending_landlord_approval() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.ACTIVE);
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        assertThatThrownBy(() -> service.approveByLandlord(MANDATE_ID, VALID_APPROVE))
                .isInstanceOf(ConflictException.class);
    }

    // ── rejectByLandlord ─────────────────────────────────────────────

    @Test
    void reject_flips_status_and_stores_reason_and_rejected_at_and_publishes_event() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.rejectByLandlord(MANDATE_ID, VALID_REJECT);

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
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.rejectByLandlord(MANDATE_ID, new RejectMandateRequest("  Too\n  steep.  "));

        assertThat(m.getRejectionReason()).isEqualTo("Too\n  steep.");
    }

    @Test
    void reject_forbidden_when_caller_is_not_the_landlord_user() {
        User otherUser = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(otherUser), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.rejectByLandlord(MANDATE_ID, VALID_REJECT))
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

    // ── V41: one non-terminal mandate per property ───────────────────

    @Test
    void issue_conflicts_when_property_already_has_a_non_terminal_mandate() {
        Property p = withId(Property.builder().title("Sandton Villa")
                .listingMode(com.habitat.api.enums.ListingMode.AGENT_MANAGED)
                .build(), PROP_ID);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        // existsByProperty_*StatusNotIn is the new V41 gate. When true,
        // the service throws ConflictException before resolving the
        // landlord or building the row — so the heavier mocks
        // (landlordService, users, mandatePdf, storage) aren't needed.
        when(mandates.existsByProperty_IdAndStatusNotIn(
                eq(PROP_ID),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);

        var req = new com.habitat.api.dto.mandate.IssueMandateRequest(
                MandateType.FULL_MANAGEMENT, new BigDecimal("8.0"),
                null, "Naledi", "Owner", "naledi@example.co.za", null, null);

        assertThatThrownBy(() -> service.issue(PROP_ID, req))
                .isInstanceOf(ConflictException.class);
        verify(mandates, never()).save(any());
    }

    // ── Slice 4: requestChanges / resubmit / withdraw / history ──────

    @Test
    void request_changes_snapshots_current_value_server_side_and_flips_status() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);
        when(changeRequests.save(any(MandateChangeRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var req = new RequestChangesRequest(
                List.of(new ChangeItemRequest(ChangeRequestField.FEE, "6")),
                "Too high for tenant-find.");

        service.requestChanges(MANDATE_ID, req);

        ArgumentCaptor<MandateChangeRequest> captor =
                ArgumentCaptor.forClass(MandateChangeRequest.class);
        verify(changeRequests).save(captor.capture());
        MandateChangeRequest saved = captor.getValue();
        assertThat(saved.getItems()).hasSize(1);
        // Server-side snapshot of current fee, not whatever the client
        // might have passed: m's fee is "8.0".
        assertThat(saved.getItems().get(0).currentValue()).isEqualTo("8.0");
        assertThat(saved.getItems().get(0).requestedValue()).isEqualTo("6");
        assertThat(saved.getStatus()).isEqualTo(ChangeRequestStatus.OPEN);
        assertThat(m.getStatus()).isEqualTo(MandateStatus.CHANGES_REQUESTED);
        verify(events).publishEvent(any(MandateChangesRequestedEvent.class));
    }

    @Test
    void request_changes_conflict_when_status_is_not_pending_landlord_approval() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(owner), MandateStatus.ACTIVE);
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        assertThatThrownBy(() ->
                service.requestChanges(MANDATE_ID,
                        new RequestChangesRequest(
                                List.of(new ChangeItemRequest(ChangeRequestField.FEE, "6")),
                                null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void resubmit_applies_selective_patch_marks_addressed_and_flips_status() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        User agent = user(AGENT_ID, "Pieter", "Agent");
        Mandate m = mandate(agent, onlineLandlord(owner), MandateStatus.CHANGES_REQUESTED);
        MandateChangeRequest open = MandateChangeRequest.builder()
                .mandate(m).status(ChangeRequestStatus.OPEN).build();
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(changeRequests.findByMandate_IdAndStatus(MANDATE_ID, ChangeRequestStatus.OPEN))
                .thenReturn(List.of(open));
        when(security.requireUserId()).thenReturn(AGENT_ID);
        when(users.findById(AGENT_ID)).thenReturn(Optional.of(agent));

        var req = new ResubmitMandateRequest(null, new BigDecimal("6.00"), null);
        service.resubmit(MANDATE_ID, req);

        assertThat(m.getFeePercent()).isEqualByComparingTo("6.00");
        assertThat(m.getStatus()).isEqualTo(MandateStatus.PENDING_LANDLORD_APPROVAL);
        assertThat(open.getStatus()).isEqualTo(ChangeRequestStatus.ADDRESSED);
        assertThat(open.getResolvedAt()).isNotNull();
        assertThat(open.getResolvedByUser()).isEqualTo(agent);
        verify(events).publishEvent(any(MandateResubmittedEvent.class));
    }

    @Test
    void resubmit_conflict_when_status_is_pending_landlord_approval() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        Mandate m = mandate(agent, onlineLandlord(user(OWNER_ID, "Naledi", "Owner")),
                MandateStatus.PENDING_LANDLORD_APPROVAL);
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.resubmit(MANDATE_ID,
                new ResubmitMandateRequest(null, new BigDecimal("6.00"), null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void withdraw_flips_rejected_stores_reason_and_marks_open_change_requests_withdrawn() {
        User agent = user(AGENT_ID, "Pieter", "Agent");
        Mandate m = mandate(agent, onlineLandlord(user(OWNER_ID, "Naledi", "Owner")),
                MandateStatus.CHANGES_REQUESTED);
        MandateChangeRequest open = MandateChangeRequest.builder()
                .mandate(m).status(ChangeRequestStatus.OPEN).build();
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));
        when(changeRequests.findByMandate_IdAndStatus(MANDATE_ID, ChangeRequestStatus.OPEN))
                .thenReturn(List.of(open));
        when(security.requireUserId()).thenReturn(AGENT_ID);

        service.withdraw(MANDATE_ID,
                new WithdrawMandateRequest("Cannot meet your fee — withdrawing."));

        assertThat(m.getStatus()).isEqualTo(MandateStatus.REJECTED);
        assertThat(m.getWithdrawnReason()).isEqualTo("Cannot meet your fee — withdrawing.");
        assertThat(m.getWithdrawnAt()).isNotNull();
        assertThat(m.getWithdrawnByUserId()).isEqualTo(AGENT_ID);
        assertThat(open.getStatus()).isEqualTo(ChangeRequestStatus.WITHDRAWN);
        verify(events).publishEvent(any(MandateWithdrawnEvent.class));
    }

    @Test
    void withdraw_conflict_when_status_is_active() {
        Mandate m = mandate(user(AGENT_ID, "Pieter", "Agent"),
                onlineLandlord(user(OWNER_ID, "Naledi", "Owner")), MandateStatus.ACTIVE);
        when(mandates.findById(MANDATE_ID))
                .thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.withdraw(MANDATE_ID,
                new WithdrawMandateRequest("withdrawing")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void history_emits_ordered_events_across_a_round_trip() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        User agent = user(AGENT_ID, "Pieter", "Agent");
        Mandate m = mandate(agent, onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        setField(m, "createdAt", java.time.OffsetDateTime.parse("2026-05-01T08:00:00Z"));

        MandateChangeRequest addressedCr = MandateChangeRequest.builder()
                .mandate(m)
                .requestedByUser(owner)
                .requestedAt(java.time.OffsetDateTime.parse("2026-05-02T08:00:00Z"))
                .resolvedAt(java.time.OffsetDateTime.parse("2026-05-03T08:00:00Z"))
                .resolvedByUser(agent)
                .status(ChangeRequestStatus.ADDRESSED)
                .items(List.of())
                .build();
        when(mandates.findByProperty_IdOrderByCreatedAtAsc(PROP_ID))
                .thenReturn(List.of(m));
        when(changeRequests.findByMandate_Property_IdOrderByRequestedAtAsc(PROP_ID))
                .thenReturn(List.of(addressedCr));

        MandateHistoryResponse out = service.getHistory(PROP_ID);

        assertThat(out.events()).hasSize(3);
        assertThat(out.events().get(0).kind().name()).isEqualTo("ISSUED");
        assertThat(out.events().get(1).kind().name()).isEqualTo("CHANGES_REQUESTED");
        assertThat(out.events().get(2).kind().name()).isEqualTo("RESUBMITTED");
    }

    @Test
    void history_spans_terminal_rejection_then_fresh_issue_across_two_mandate_rows() {
        User owner = user(OWNER_ID, "Naledi", "Owner");
        User agent = user(AGENT_ID, "Pieter", "Agent");

        // Round 1: REJECTED.
        Mandate prior = mandate(agent, onlineLandlord(owner), MandateStatus.REJECTED);
        setField(prior, "id", UUID.fromString("11111111-aaaa-bbbb-cccc-111111111111"));
        setField(prior, "createdAt", java.time.OffsetDateTime.parse("2026-05-01T08:00:00Z"));
        prior.setRejectionReason("Fee too high.");
        prior.setRejectedAt(java.time.OffsetDateTime.parse("2026-05-02T08:00:00Z"));

        // Round 2: fresh issue, PENDING.
        Mandate current = mandate(agent, onlineLandlord(owner), MandateStatus.PENDING_LANDLORD_APPROVAL);
        setField(current, "id", UUID.fromString("22222222-aaaa-bbbb-cccc-222222222222"));
        setField(current, "createdAt", java.time.OffsetDateTime.parse("2026-05-03T08:00:00Z"));

        when(mandates.findByProperty_IdOrderByCreatedAtAsc(PROP_ID))
                .thenReturn(List.of(prior, current));
        when(changeRequests.findByMandate_Property_IdOrderByRequestedAtAsc(PROP_ID))
                .thenReturn(List.of());

        MandateHistoryResponse out = service.getHistory(PROP_ID);

        // ISSUED(prior) → REJECTED(prior) → ISSUED(current). Three
        // events on one timeline spanning the two mandate rows.
        assertThat(out.events()).hasSize(3);
        assertThat(out.events().get(0).kind().name()).isEqualTo("ISSUED");
        assertThat(out.events().get(1).kind().name()).isEqualTo("REJECTED");
        assertThat(out.events().get(2).kind().name()).isEqualTo("ISSUED");
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
        setField(entity, "id", id);
    }

    private static void setField(Object entity, String fieldName, Object value) {
        try {
            Class<?> c = entity.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(entity, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    c = c.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
