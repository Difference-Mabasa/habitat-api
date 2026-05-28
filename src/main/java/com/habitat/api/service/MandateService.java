package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.constants.StorageConstants;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.mandate.ApproveMandateRequest;
import com.habitat.api.dto.mandate.IssueMandateRequest;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.ListingMode;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.event.MandateActiveEvent;
import com.habitat.api.event.MandateApprovedEvent;
import com.habitat.api.event.MandateIssuedEvent;
import com.habitat.api.event.MandateRejectedEvent;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
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

import java.time.OffsetDateTime;
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

    private final MandateRepository mandates;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final PropertyService propertyService;
    private final LandlordService landlordService;
    private final MandatePdfService mandatePdf;
    private final StorageService storage;
    private final SecurityUtils security;
    private final ApplicationEventPublisher events;

    /** Most-recent mandate for the property (any status), if any. */
    @Transactional(readOnly = true)
    public Optional<MandateResponse> getForProperty(UUID propertyId) {
        return mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .map(MandateResponse::from);
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

        Optional<Mandate> existing = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId);
        if (existing.isPresent() && existing.get().getStatus() == MandateStatus.ACTIVE) {
            throw new ConflictException(ErrorMessages.MANDATE_NOT_READY_FOR_UPLOAD);
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
    public MandateResponse approveByLandlord(UUID propertyId, ApproveMandateRequest req) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
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
     * Online landlord rejects the mandate. Caller must be the
     * resolved owner User on {@code property.landlord} — anyone
     * else gets 403. Transitions PENDING_LANDLORD_APPROVAL → REJECTED
     * and publishes {@link MandateRejectedEvent} so the agent can
     * follow up out-of-band before re-issuing.
     */
    @Transactional
    public MandateResponse rejectByLandlord(UUID propertyId) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        requireLandlordCaller(m);
        if (m.getStatus() != MandateStatus.PENDING_LANDLORD_APPROVAL) {
            throw new ConflictException(ErrorMessages.MANDATE_NOT_READY_FOR_LANDLORD_DECISION);
        }
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
    public MandateResponse uploadSigned(UUID propertyId, MultipartFile file) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
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
    public void emailToLandlord(UUID propertyId) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
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

    /** Streaming handle for the generated mandate PDF. */
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
     * on-demand Playwright render. Returns nothing — the call's only
     * job is to throw {@code 404} or {@code 403} when appropriate.
     */
    @Transactional(readOnly = true)
    public void requirePdfReadable(UUID propertyId) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());
    }

    /** Streaming handle for the signed mandate (offline flow). */
    @Transactional(readOnly = true)
    public StoredResource openSignedPdf(UUID propertyId) {
        Mandate m = mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.MANDATE_NOT_FOUND));
        propertyService.requireCanEdit(m.getProperty());
        String path = m.getSignedDocumentPath();
        if (path == null) throw new ConflictException(ErrorMessages.MANDATE_SIGNED_NOT_AVAILABLE);
        return storage.open(path);
    }
}
