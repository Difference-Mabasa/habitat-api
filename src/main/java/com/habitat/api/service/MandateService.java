package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.constants.StorageConstants;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.mandate.ApproveMandateRequest;
import com.habitat.api.dto.mandate.ChangeItemRequest;
import com.habitat.api.dto.mandate.HistoryEventResponse;
import com.habitat.api.dto.mandate.IssueMandateRequest;
import com.habitat.api.dto.mandate.MandateHistoryResponse;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.dto.mandate.RejectMandateRequest;
import com.habitat.api.dto.mandate.RequestChangesRequest;
import com.habitat.api.dto.mandate.ResubmitMandateRequest;
import com.habitat.api.dto.mandate.WithdrawMandateRequest;
import com.habitat.api.entity.ChangeItem;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.MandateChangeRequest;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ChangeRequestStatus;
import com.habitat.api.enums.HistoryEventKind;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.ListingMode;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.event.MandateActiveEvent;
import com.habitat.api.event.MandateApprovedEvent;
import com.habitat.api.event.MandateChangesRequestedEvent;
import com.habitat.api.event.MandateIssuedEvent;
import com.habitat.api.event.MandateRejectedEvent;
import com.habitat.api.event.MandateResubmittedEvent;
import com.habitat.api.event.MandateWithdrawnEvent;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.MandateChangeRequestRepository;
import com.habitat.api.repository.MandateRepository;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.habitat.api.storage.StorageService;
import com.habitat.api.storage.StoredFile;
import com.habitat.api.storage.StoredResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Mandate lifecycle for agent-managed listings. Landlord identity is
 * resolved via {@link LandlordService} (find-or-create by SA ID) and
 * pinned onto {@code property.landlord}, so subsequent mandates on
 * the same property pick up the same Landlord row. Online vs offline
 * flow is then decided by the resolved Landlord's type:
 *
 * <ul>
 *   <li>{@code ONLINE} — the captured email matched a Habitat user
 *       (or the ID number matched an already-online row); status
 *       becomes {@code PENDING_LANDLORD_APPROVAL}.</li>
 *   <li>{@code OFFLINE} — captured contact details only; status
 *       becomes {@code PENDING_OFFLINE_SIGNATURE}. The agent
 *       downloads the generated PDF, emails it to the landlord (or
 *       hands it over), then re-uploads the signed version via
 *       {@link #uploadSigned}, which flips status to
 *       {@code PENDING_AGENT_ACCEPTANCE}.</li>
 * </ul>
 *
 * <p>Email delivery (Phase 8 Resend) isn't wired yet; for now
 * {@link #emailToLandlord} logs and returns success so the UI button
 * shows green. The agent always has the option to download the PDF
 * directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MandateService {

    /** Statuses that take a mandate out of the in-flight set. Single
     *  source of truth — used both as the V41 invariant filter and
     *  as the gate on every action endpoint's "find current" lookup. */
    private static final java.util.List<MandateStatus> TERMINAL_STATUSES =
            java.util.List.of(MandateStatus.REJECTED, MandateStatus.EXPIRED);

    private final MandateRepository mandates;
    private final MandateChangeRequestRepository changeRequests;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final PropertyService propertyService;
    private final LandlordService landlordService;
    private final MandatePdfService mandatePdf;
    private final StorageService storage;
    private final SecurityUtils security;
    private final ApplicationEventPublisher events;

    /** Most-recent mandate for the property (any status), if any.
     *  Includes the latest OPEN change request when relevant so the
     *  detail screens don't need a second round-trip. */
    @Transactional(readOnly = true)
    public Optional<MandateResponse> getForProperty(UUID propertyId) {
        return mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .map(m -> MandateResponse.from(m, latestOpenChangeRequest(m)));
    }

    private MandateChangeRequest latestOpenChangeRequest(Mandate m) {
        if (m == null) return null;
        return changeRequests
                .findFirstByMandate_IdAndStatusOrderByRequestedAtDesc(m.getId(), ChangeRequestStatus.OPEN)
                .orElse(null);
    }

    /**
     * Mandates pending the calling user's landlord-side approval.
     * Drives the /mandate-approvals inbox the
     * MANDATE_PENDING_LANDLORD_APPROVAL notification's CTA lands on.
     * Returns an empty page when the caller is OFFLINE or has no
     * pending mandates. Page size is capped at 100 (mirrors
     * {@link PropertyService#listManagedByMe}); the global
     * PageSizeFilter enforces the same ceiling on the wire.
     */
    @Transactional(readOnly = true)
    public PageResponse<MandateResponse> listAwaitingMyApproval(int page, int size) {
        UUID me = security.requireUserId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<Mandate> p = mandates.findByStatusAndProperty_Landlord_User_IdOrderByCreatedAtDesc(
                MandateStatus.PENDING_LANDLORD_APPROVAL, me, pageable);
        return PageResponse.from(p.map(MandateResponse::from));
    }

    /**
     * Create or replace the mandate on a property. Re-issuing on top
     * of a terminal status (REJECTED / EXPIRED) is fine — we just
     * write a new row. Re-issuing on an active mandate currently
     * 409s; agents who want to update fee / type re-mandate via a
     * future endpoint (out of scope for this slice).
     */
    @Transactional
    public MandateResponse issue(UUID propertyId, IssueMandateRequest req) {
        Property p = properties.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PROPERTY_NOT_FOUND));
        propertyService.requireCanEdit(p);

        if (p.getListingMode() != ListingMode.AGENT_MANAGED) {
            throw new ConflictException(ErrorMessages.MANDATE_REQUIRES_AGENT_MODE);
        }

        // V41 invariant: at most one non-terminal mandate per property.
        // Terminal rows (REJECTED, EXPIRED) stay around as history, so
        // a fresh issue after rejection is fine — the new row sits
        // alongside the old terminal one. Concurrent in-flight
        // mandates are blocked here AND by the partial unique index;
        // the service check exists to surface a clean 409 instead of
        // a 500-shaped constraint violation.
        boolean hasNonTerminal = mandates.existsByProperty_IdAndStatusNotIn(
                propertyId, TERMINAL_STATUSES);
        if (hasNonTerminal) {
            throw new ConflictException(ErrorMessages.MANDATE_ALREADY_IN_FLIGHT);
        }

        UUID agentId = security.requireUserId();
        User agent = users.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));

        // Resolve the Landlord row first — find-or-create by SA ID,
        // then fall back to email-matched user (ONLINE) or captured
        // fields (OFFLINE). Pinning it on the property happens before
        // we build the mandate row so a downstream failure (e.g. PDF
        // render) leaves the landlord linked for the retry.
        Landlord landlord = landlordService.resolveForMandate(
                new LandlordService.MandateLandlordCapture(
                        req.landlordIdNumber(),
                        req.landlordFirstName(),
                        req.landlordLastName(),
                        req.landlordEmail(),
                        req.landlordPhone()));
        p.setLandlord(landlord);

        boolean online = landlord.getType() == LandlordType.ONLINE;
        Mandate mandate = Mandate.builder()
                .property(p)
                .agent(agent)
                .mandateType(req.mandateType())
                .status(online
                        ? MandateStatus.PENDING_LANDLORD_APPROVAL
                        : MandateStatus.PENDING_OFFLINE_SIGNATURE)
                .feePercent(req.feePercent())
                .notes(req.notes())
                .build();
        Mandate saved = mandates.save(mandate);

        // Render + persist the PDF AFTER saving so the file's reference
        // can use the row id. Any render failure aborts the txn so we
        // don't leave a row without a backing PDF.
        byte[] pdf = mandatePdf.render(saved);
        StoredFile stored = storage.storeTrustedBytes(
                StorageConstants.FOLDER_LEASES,
                "mandate-" + saved.getId() + ".pdf",
                "application/pdf",
                pdf);
        saved.setMandateDocumentPath(stored.storedPath());

        log.info("mandate {} issued for property {} (status={})",
                saved.getId(), propertyId, saved.getStatus());
        events.publishEvent(new MandateIssuedEvent(saved.getId()));
        return MandateResponse.from(saved);
    }

    /**
     * Online landlord approves the mandate by typing their full
     * registered name as the e-signature. Caller must be the resolved
     * owner User on {@code property.landlord} — anyone else gets 403.
     * The typed name is normalised (trimmed, internal whitespace
     * collapsed, case-folded) and compared against the landlord's
     * registered first + surname; mismatch is a 400 so accidental
     * clicks / scripted approvals can't slip through even if the
     * client-side check is bypassed.
     *
     * <p>Transitions PENDING_LANDLORD_APPROVAL → ACTIVE and stores
     * the typed name + server timestamp on the row for the audit
     * trail. Publishes {@link MandateApprovedEvent} + {@link MandateActiveEvent}
     * (terminal-state acknowledgement) — unchanged from the
     * pre-signing path.
     */
    @Transactional
    public MandateResponse approveByLandlord(UUID mandateId, ApproveMandateRequest req) {
        Mandate m = mandates.findById(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        requireLandlordCaller(m);
        if (m.getStatus() != MandateStatus.PENDING_LANDLORD_APPROVAL) {
            throw new ConflictException(ErrorMessages.MANDATE_NOT_READY_FOR_LANDLORD_DECISION);
        }

        User landlordUser = m.getProperty().getLandlord().getUser();
        String expected = normaliseName(
                (landlordUser.getFirstName() == null ? "" : landlordUser.getFirstName())
                        + " "
                        + (landlordUser.getSurname() == null ? "" : landlordUser.getSurname()));
        String typed = normaliseName(req.signedName());
        if (expected.isEmpty() || !expected.equals(typed)) {
            throw new BadRequestException(ErrorMessages.MANDATE_SIGNED_NAME_MISMATCH);
        }

        m.setSignedName(req.signedName().trim());
        m.setSignedAt(OffsetDateTime.now());
        m.setStatus(MandateStatus.ACTIVE);
        log.info("mandate {} approved + e-signed by landlord — status ACTIVE", m.getId());
        events.publishEvent(new MandateApprovedEvent(m.getId()));
        events.publishEvent(new MandateActiveEvent(m.getId()));
        return MandateResponse.from(m);
    }

    private static String normaliseName(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Online landlord rejects the mandate with a free-text reason
     * that the agent reads before deciding whether to revise + re-issue
     * (slice 4) or accept the rejection. Caller must be the resolved
     * owner User on {@code property.landlord} — anyone else gets 403.
     * The DTO enforces 4–1000 char length via jakarta validation so
     * a blank submit gets a 422 before it reaches the service.
     *
     * <p>Transitions PENDING_LANDLORD_APPROVAL → REJECTED, stores the
     * reason + server timestamp for the audit trail. Publishes
     * {@link MandateRejectedEvent} unchanged.
     */
    @Transactional
    public MandateResponse rejectByLandlord(UUID mandateId, RejectMandateRequest req) {
        Mandate m = mandates.findById(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        requireLandlordCaller(m);
        if (m.getStatus() != MandateStatus.PENDING_LANDLORD_APPROVAL) {
            throw new ConflictException(ErrorMessages.MANDATE_NOT_READY_FOR_LANDLORD_DECISION);
        }
        m.setRejectionReason(req.reason().trim());
        m.setRejectedAt(OffsetDateTime.now());
        m.setStatus(MandateStatus.REJECTED);
        log.info("mandate {} rejected by landlord — status REJECTED", m.getId());
        events.publishEvent(new MandateRejectedEvent(m.getId()));
        return MandateResponse.from(m);
    }

    /** Only the resolved online owner can approve/reject. */
    private void requireLandlordCaller(Mandate m) {
        UUID me = security.requireUserId();
        Property p = m.getProperty();
        Landlord landlord = p == null ? null : p.getLandlord();
        boolean isOnlineOwner = landlord != null
                && landlord.getType() == LandlordType.ONLINE
                && landlord.getUser() != null
                && me.equals(landlord.getUser().getId());
        if (!isOnlineOwner) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
    }

    /**
     * Agent uploads the offline-signed mandate. Flips status to
     * {@code PENDING_AGENT_ACCEPTANCE} (or {@code ACTIVE} when the
     * agent has already attested).
     */
    @Transactional
    public MandateResponse uploadSigned(UUID mandateId, MultipartFile file) {
        Mandate m = mandates.findById(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());

        if (m.getStatus() != MandateStatus.PENDING_OFFLINE_SIGNATURE
                && m.getStatus() != MandateStatus.PENDING_AGENT_ACCEPTANCE) {
            throw new ConflictException(ErrorMessages.MANDATE_NOT_READY_FOR_UPLOAD);
        }

        StoredFile stored = storage.store(
                StorageConstants.FOLDER_LEASES,
                file,
                StorageConstants.ALLOWED_DOCUMENT_TYPES,
                StorageConstants.MAX_DOCUMENT_BYTES);

        // Clean up any prior signed upload before swapping pointers.
        if (m.getSignedDocumentPath() != null && !m.getSignedDocumentPath().isBlank()) {
            storage.delete(m.getSignedDocumentPath());
        }
        m.setSignedDocumentPath(stored.storedPath());
        MandateStatus next = m.isAgentAttested()
                ? MandateStatus.ACTIVE
                : MandateStatus.PENDING_AGENT_ACCEPTANCE;
        m.setStatus(next);
        log.info("mandate {} signed PDF uploaded; status={}", m.getId(), m.getStatus());
        if (next == MandateStatus.ACTIVE) {
            events.publishEvent(new MandateActiveEvent(m.getId()));
        }
        return MandateResponse.from(m);
    }

    /**
     * Stub email — real Resend delivery lands in Phase 8. For now we
     * log + return the response so the UI button shows success.
     */
    @Transactional(readOnly = true)
    public void emailToLandlord(UUID mandateId) {
        Mandate m = mandates.findById(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());
        if (m.getMandateDocumentPath() == null) {
            throw new ConflictException(ErrorMessages.MANDATE_PDF_NOT_READY);
        }
        var landlord = m.getProperty() == null ? null : m.getProperty().getLandlord();
        String email = landlord == null ? null : landlord.resolvedEmail();
        log.info("[mandate-email stub] would email mandate {} to {}", m.getId(), email);
        // Real delivery: Resend integration ships in Phase 8.
    }

    /** Streaming handle for the generated mandate PDF. Reads the
     *  most-recent row (in-flight if one exists, else the latest
     *  terminal) so the download mirrors what the detail screen is
     *  showing — an agent may legitimately want a PDF of a historical
     *  rejected mandate for their records. */
    @Transactional(readOnly = true)
    public StoredResource openMandatePdf(UUID propertyId) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());
        String path = m.getMandateDocumentPath();
        if (path == null) throw new ConflictException(ErrorMessages.MANDATE_PDF_NOT_READY);
        return storage.open(path);
    }

    /**
     * Access check the {@code /mandate/pdf} download endpoint runs
     * before delegating to {@link BrowserRendererService} for an
     * on-demand Playwright render. Mirrors {@link #openMandatePdf} —
     * accepts the most-recent row so historical PDFs can still be
     * pulled.
     */
    @Transactional(readOnly = true)
    public void requirePdfReadable(UUID propertyId) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());
    }

    /** Streaming handle for the signed mandate (offline flow). Same
     *  most-recent semantics as {@link #openMandatePdf}. */
    @Transactional(readOnly = true)
    public StoredResource openSignedPdf(UUID propertyId) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());
        String path = m.getSignedDocumentPath();
        if (path == null) throw new ConflictException(ErrorMessages.MANDATE_SIGNED_NOT_AVAILABLE);
        return storage.open(path);
    }

    // ── Slice 4 — request-changes / resubmit / withdraw / history ─────────

    /**
     * Online landlord asks the agent to revise the mandate before
     * signing. The structured items are snapshotted server-side
     * (currentValue derived from the mandate, not trusted from the
     * client) so the audit trail reflects reality. Status flips
     * PENDING_LANDLORD_APPROVAL → CHANGES_REQUESTED.
     */
    @Transactional
    public MandateResponse requestChanges(UUID mandateId, RequestChangesRequest req) {
        Mandate m = mandates.findById(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        requireLandlordCaller(m);
        if (m.getStatus() != MandateStatus.PENDING_LANDLORD_APPROVAL) {
            throw new ConflictException(ErrorMessages.MANDATE_NOT_READY_FOR_LANDLORD_DECISION);
        }

        User landlordUser = m.getProperty().getLandlord().getUser();
        List<ChangeItem> snapshot = snapshotItems(m, req.items());

        MandateChangeRequest cr = MandateChangeRequest.builder()
                .mandate(m)
                .requestedByUser(landlordUser)
                .requestedAt(OffsetDateTime.now())
                .comment(req.comment() == null ? null : req.comment().trim())
                .items(snapshot)
                .status(ChangeRequestStatus.OPEN)
                .build();
        MandateChangeRequest saved = changeRequests.save(cr);

        m.setStatus(MandateStatus.CHANGES_REQUESTED);
        log.info("mandate {} — landlord requested {} changes (cr {})",
                m.getId(), snapshot.size(), saved.getId());
        events.publishEvent(new MandateChangesRequestedEvent(m.getId(), saved.getId()));
        return MandateResponse.from(m, saved);
    }

    private static List<ChangeItem> snapshotItems(Mandate m, List<ChangeItemRequest> reqItems) {
        if (reqItems == null) return List.of();
        List<ChangeItem> out = new ArrayList<>(reqItems.size());
        for (ChangeItemRequest r : reqItems) {
            String current = switch (r.field()) {
                case FEE -> m.getFeePercent() == null ? "" : m.getFeePercent().toPlainString();
                case SCOPE -> m.getMandateType() == null ? "" : m.getMandateType().name();
                case NOTES -> m.getNotes() == null ? "" : m.getNotes();
            };
            out.add(new ChangeItem(r.field(), current, r.requestedValue()));
        }
        return out;
    }

    /**
     * Agent applies a selective patch to the mandate and resubmits
     * for the landlord's approval. Any OPEN change request is marked
     * ADDRESSED. Status flips CHANGES_REQUESTED → PENDING_LANDLORD_APPROVAL.
     */
    @Transactional
    public MandateResponse resubmit(UUID mandateId, ResubmitMandateRequest req) {
        Mandate m = mandates.findById(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());
        if (m.getStatus() != MandateStatus.CHANGES_REQUESTED) {
            throw new ConflictException(ErrorMessages.MANDATE_NOT_READY_FOR_RESUBMIT);
        }

        if (req.mandateType() != null) m.setMandateType(req.mandateType());
        if (req.feePercent() != null)  m.setFeePercent(req.feePercent());
        if (req.notes() != null)       m.setNotes(req.notes().trim());

        UUID agentId = security.requireUserId();
        User agent = users.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
        OffsetDateTime now = OffsetDateTime.now();
        for (MandateChangeRequest cr : changeRequests.findByMandate_IdAndStatus(
                m.getId(), ChangeRequestStatus.OPEN)) {
            cr.setStatus(ChangeRequestStatus.ADDRESSED);
            cr.setResolvedAt(now);
            cr.setResolvedByUser(agent);
        }

        m.setStatus(MandateStatus.PENDING_LANDLORD_APPROVAL);
        log.info("mandate {} resubmitted by agent {} — status PENDING_LANDLORD_APPROVAL",
                m.getId(), agentId);
        events.publishEvent(new MandateResubmittedEvent(m.getId()));
        return MandateResponse.from(m, null);
    }

    /**
     * Agent withdraws the mandate from any pending state. Mirrors
     * slice 3's reject path but the actor is the agent — separate
     * audit fields keep the two distinguishable.
     */
    @Transactional
    public MandateResponse withdraw(UUID mandateId, WithdrawMandateRequest req) {
        Mandate m = mandates.findById(mandateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());
        MandateStatus s = m.getStatus();
        if (s != MandateStatus.PENDING_LANDLORD_APPROVAL && s != MandateStatus.CHANGES_REQUESTED) {
            throw new ConflictException(ErrorMessages.MANDATE_NOT_READY_FOR_WITHDRAW);
        }

        UUID agentId = security.requireUserId();
        OffsetDateTime now = OffsetDateTime.now();
        m.setWithdrawnReason(req.reason().trim());
        m.setWithdrawnAt(now);
        m.setWithdrawnByUserId(agentId);
        m.setStatus(MandateStatus.REJECTED);

        for (MandateChangeRequest cr : changeRequests.findByMandate_IdAndStatus(
                m.getId(), ChangeRequestStatus.OPEN)) {
            cr.setStatus(ChangeRequestStatus.WITHDRAWN);
            cr.setResolvedAt(now);
        }

        log.info("mandate {} withdrawn by agent {} — status REJECTED",
                m.getId(), agentId);
        events.publishEvent(new MandateWithdrawnEvent(m.getId()));
        return MandateResponse.from(m, null);
    }

    /** Chronological event timeline spanning every mandate row this
     *  property has ever had (including terminal historical ones).
     *  V41 enforces at most one non-terminal row per property, but
     *  re-mandating after rejection inserts a fresh row alongside the
     *  old REJECTED one — both need to surface on the same timeline
     *  for the landlord and agent to understand the full back-and-forth. */
    @Transactional(readOnly = true)
    public MandateHistoryResponse getHistory(UUID propertyId) {
        List<Mandate> rows = mandates.findByProperty_IdOrderByCreatedAtAsc(propertyId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND);
        }
        // Authorise once on any row's property — they all point to the same one.
        propertyService.requireCanEdit(rows.get(0).getProperty());

        // Batch every change request across every round in one query
        // and bucket by mandate id so the per-row loop below stays
        // O(rows + crs) instead of fanning out queries.
        Map<UUID, List<MandateChangeRequest>> crsByMandate = new HashMap<>();
        for (MandateChangeRequest cr : changeRequests.findByMandate_Property_IdOrderByRequestedAtAsc(propertyId)) {
            crsByMandate
                    .computeIfAbsent(cr.getMandate().getId(), k -> new ArrayList<>())
                    .add(cr);
        }

        List<HistoryEventResponse> events = new ArrayList<>();
        for (Mandate m : rows) {
            // ISSUED for each round
            events.add(new HistoryEventResponse(
                    HistoryEventKind.ISSUED,
                    m.getCreatedAt(),
                    m.getAgent() == null ? null : m.getAgent().getId(),
                    displayName(m.getAgent()),
                    Map.of(
                            "mandateType", m.getMandateType() == null ? "" : m.getMandateType().name(),
                            "feePercent", m.getFeePercent() == null ? "" : m.getFeePercent().toPlainString())
            ));
            // CHANGES_REQUESTED + RESUBMITTED per change request on this row
            for (MandateChangeRequest cr : crsByMandate.getOrDefault(m.getId(), List.of())) {
                User by = cr.getRequestedByUser();
                Map<String, Object> reqPayload = new HashMap<>();
                reqPayload.put("comment", cr.getComment());
                reqPayload.put("itemCount", cr.getItems() == null ? 0 : cr.getItems().size());
                events.add(new HistoryEventResponse(
                        HistoryEventKind.CHANGES_REQUESTED,
                        cr.getRequestedAt(),
                        by == null ? null : by.getId(),
                        displayName(by),
                        reqPayload));
                if (cr.getStatus() == ChangeRequestStatus.ADDRESSED && cr.getResolvedAt() != null) {
                    User resolver = cr.getResolvedByUser();
                    events.add(new HistoryEventResponse(
                            HistoryEventKind.RESUBMITTED,
                            cr.getResolvedAt(),
                            resolver == null ? null : resolver.getId(),
                            displayName(resolver),
                            Map.of("addressedItemCount",
                                    cr.getItems() == null ? 0 : cr.getItems().size())));
                }
            }
            // Terminal events for this row
            if (m.getSignedAt() != null) {
                User signer = m.getProperty().getLandlord() == null
                        ? null : m.getProperty().getLandlord().getUser();
                events.add(new HistoryEventResponse(
                        HistoryEventKind.APPROVED,
                        m.getSignedAt(),
                        signer == null ? null : signer.getId(),
                        displayName(signer),
                        Map.of("signedName", m.getSignedName() == null ? "" : m.getSignedName())));
            }
            if (m.getRejectedAt() != null && m.getWithdrawnAt() == null) {
                User rejecter = m.getProperty().getLandlord() == null
                        ? null : m.getProperty().getLandlord().getUser();
                events.add(new HistoryEventResponse(
                        HistoryEventKind.REJECTED,
                        m.getRejectedAt(),
                        rejecter == null ? null : rejecter.getId(),
                        displayName(rejecter),
                        Map.of("reason", m.getRejectionReason() == null ? "" : m.getRejectionReason())));
            }
            if (m.getWithdrawnAt() != null) {
                events.add(new HistoryEventResponse(
                        HistoryEventKind.WITHDRAWN,
                        m.getWithdrawnAt(),
                        m.getWithdrawnByUserId(),
                        displayName(m.getAgent()),
                        Map.of("reason", m.getWithdrawnReason() == null ? "" : m.getWithdrawnReason())));
            }
        }
        // Sort oldest-first for top-down rendering.
        events.sort((a, b) -> a.at().compareTo(b.at()));
        return new MandateHistoryResponse(events);
    }

    /**
     * Agent's inbox — all mandates they issued. Optional status filter
     * drives the /my-mandates filter chips on the UI.
     */
    @Transactional(readOnly = true)
    public PageResponse<MandateResponse> listMineAsAgent(MandateStatus status, int page, int size) {
        UUID me = security.requireUserId();
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(
                        Math.max(0, page), Math.max(1, Math.min(size, 100)));
        org.springframework.data.domain.Page<Mandate> p = status == null
                ? mandates.findByAgent_IdOrderByUpdatedAtDesc(me, pageable)
                : mandates.findByAgent_IdAndStatusOrderByUpdatedAtDesc(me, status, pageable);
        return PageResponse.from(p.map(MandateResponse::from));
    }

    private static String displayName(User u) {
        if (u == null) return null;
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? (u.getEmail() == null ? null : u.getEmail()) : name;
    }
}
