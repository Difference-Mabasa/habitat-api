package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.constants.StorageConstants;
import com.habitat.api.dto.mandate.IssueMandateRequest;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ListingMode;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.MandateRepository;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.storage.StorageService;
import com.habitat.api.storage.StoredFile;
import com.habitat.api.storage.StoredResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

/**
 * Mandate lifecycle for agent-managed listings. Online vs. offline
 * landlord flow is decided at issue time:
 *
 * <ul>
 *   <li>If {@code landlordEmail} resolves to an existing Habitat user
 *       we attach them as {@code landlordUser} and the status becomes
 *       {@code PENDING_LANDLORD_APPROVAL}. The mandate-approval UI
 *       (Phase 12 expansion) flips it to {@code ACTIVE}.</li>
 *   <li>Otherwise the offline fields carry the landlord's identity.
 *       Status becomes {@code PENDING_OFFLINE_SIGNATURE}; the agent
 *       downloads the generated PDF, emails it to the landlord (or
 *       hands it over), then re-uploads the signed version via
 *       {@link #uploadSigned}. That flips status to
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
    private final MandatePdfService mandatePdf;
    private final StorageService storage;
    private final SecurityUtils security;

    /** Most-recent mandate for the property (any status), if any. */
    @Transactional(readOnly = true)
    public Optional<MandateResponse> getForProperty(UUID propertyId) {
        return mandates.findFirstByProperty_IdOrderByCreatedAtDesc(propertyId)
                .map(MandateResponse::from);
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

        // Flow discriminator: a landlordName means the wizard's
        // "Landlord is on Habitat" toggle was OFF (offline flow). The
        // online flow expects the email to resolve to an existing
        // Habitat user — anything else is a hard error so the agent
        // can correct the email before publishing rather than silently
        // falling back to the offline flow with the wrong name.
        final boolean offlineFlow =
                req.landlordName() != null && !req.landlordName().isBlank();

        if (req.landlordEmail() == null || req.landlordEmail().isBlank()) {
            throw new BadRequestException(ErrorMessages.MANDATE_LANDLORD_REQUIRED);
        }

        User landlordUser = null;
        if (!offlineFlow) {
            landlordUser = users.findByEmailIgnoreCase(req.landlordEmail())
                    .orElseThrow(() -> new BadRequestException(
                            ErrorMessages.LANDLORD_EMAIL_NOT_ON_HABITAT));
        }
        final boolean online = !offlineFlow;

        Mandate mandate = Mandate.builder()
                .property(p)
                .agent(agent)
                .landlordUser(landlordUser)
                .offlineLandlordName(online ? null : req.landlordName())
                .offlineLandlordEmail(online ? null : req.landlordEmail())
                .offlineLandlordPhone(online ? null : req.landlordPhone())
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
        return MandateResponse.from(saved);
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
        m.setStatus(m.isAgentAttested() ? MandateStatus.ACTIVE : MandateStatus.PENDING_AGENT_ACCEPTANCE);
        log.info("mandate {} signed PDF uploaded; status={}", m.getId(), m.getStatus());
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
        String email = m.getLandlordUser() != null
                ? m.getLandlordUser().getEmail()
                : m.getOfflineLandlordEmail();
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
