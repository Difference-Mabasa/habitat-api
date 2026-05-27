package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.constants.StorageConstants;
import com.habitat.api.dto.application.ApplicationDocumentResponse;
import com.habitat.api.dto.application.ApplicationResponse;
import com.habitat.api.dto.application.CreateApplicationRequest;
import com.habitat.api.dto.application.RequiredDocumentsResponse;
import com.habitat.api.dto.application.ReviewApplicationRequest;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.ApplicationDocument;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.DocumentType;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.ApplicationDocumentRepository;
import com.habitat.api.repository.ApplicationRepository;
import com.habitat.api.repository.PropertyRequiredDocumentRepository;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.storage.StorageService;
import com.habitat.api.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tenant-facing application service. Owns create + document upload +
 * the required-documents lookup that drives the upload-screen checklist.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applications;
    private final UnitRepository units;
    private final UserRepository users;
    private final PropertyRequiredDocumentRepository requiredDocs;
    private final ApplicationDocumentRepository appDocs;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final SecurityUtils security;
    private final StorageService storage;

    @Transactional
    public ApplicationResponse create(CreateApplicationRequest req) {
        UUID me = security.requireUserId();

        Unit unit = units.findById(req.unitId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.UNIT_NOT_FOUND));

        if (unit.getStatus() != UnitStatus.AVAILABLE) {
            throw new ConflictException(ErrorMessages.UNIT_NOT_AVAILABLE);
        }

        if (applications.existsByUnit_IdAndTenant_Id(unit.getId(), me)) {
            throw new ConflictException(ErrorMessages.APPLICATION_ALREADY_SUBMITTED);
        }

        User tenant = users.findById(me)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));

        // Auto-transition mirrors backroom: if the property has required
        // docs, the application lands at AWAITING_DOCUMENTS so the UI can
        // jump straight to the upload screen. Otherwise it stays SUBMITTED
        // and the tenant just waits for the landlord.
        UUID propertyId = unit.getProperty().getId();
        boolean propertyNeedsDocs = !requiredDocs.findByProperty_Id(propertyId).isEmpty();
        ApplicationStatus initialStatus = propertyNeedsDocs
                ? ApplicationStatus.AWAITING_DOCUMENTS
                : ApplicationStatus.SUBMITTED;

        Application application = Application.builder()
                .unit(unit)
                .tenant(tenant)
                .status(initialStatus)
                .message(req.message())
                .moveInDate(req.moveInDate())
                .employmentStatus(req.employmentStatus())
                .build();

        Application saved = applications.save(application);
        log.info("application {} submitted by tenant {} for unit {} (status={})",
                saved.getId(), tenant.getId(), unit.getId(), initialStatus);

        // AFTER_COMMIT listener fans out notifications to tenant + manager
        // + landlord (if distinct). Keeps ApplicationService decoupled
        // from NotificationService (TECH_DEBT ARCH-03).
        events.publishEvent(new com.habitat.api.event.ApplicationSubmittedEvent(saved.getId()));

        return ApplicationResponse.from(saved);
    }

    /** What the upload-screen checklist asks for, plus what's already been uploaded. */
    @Transactional(readOnly = true)
    public RequiredDocumentsResponse listRequiredDocuments(UUID applicationId) {
        UUID me = security.requireUserId();
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.APPLICATION_NOT_FOUND));
        requireOwnerOrThrow(application, me);

        UUID propertyId = application.getUnit().getProperty().getId();
        List<DocumentType> required = requiredDocs.findByProperty_Id(propertyId).stream()
                .map(com.habitat.api.entity.PropertyRequiredDocument::getDocType)
                .toList();
        List<ApplicationDocumentResponse> uploaded = appDocs
                .findByApplication_Id(applicationId)
                .stream()
                .map(ApplicationDocumentResponse::from)
                .toList();
        return new RequiredDocumentsResponse(required, uploaded);
    }

    /**
     * Records (or replaces) a single uploaded document for an application.
     * Until the real StorageService lands the controller passes through
     * a stub {@code fileUrl}; this method is shape-stable so swapping in
     * multipart later won't break call sites.
     *
     * <p>When the upload completes the set of required types, the
     * application auto-transitions to {@code DOCUMENTS_SUBMITTED}.
     */
    @Transactional
    public ApplicationDocumentResponse uploadDocument(UUID applicationId,
                                                      DocumentType docType,
                                                      MultipartFile file) {
        UUID me = security.requireUserId();
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.APPLICATION_NOT_FOUND));
        requireOwnerOrThrow(application, me);

        if (application.getStatus() != ApplicationStatus.AWAITING_DOCUMENTS
                && application.getStatus() != ApplicationStatus.DOCUMENTS_SUBMITTED) {
            throw new ConflictException(ErrorMessages.DOCS_UPLOAD_WRONG_STATUS);
        }

        // Validate + persist the bytes BEFORE touching the row. If Tika
        // rejects the upload we never create a row pointing at nothing.
        StoredFile stored = storage.store(
                StorageConstants.FOLDER_DOCUMENTS,
                file,
                StorageConstants.ALLOWED_DOCUMENT_TYPES,
                StorageConstants.MAX_DOCUMENT_BYTES);

        ApplicationDocument doc = appDocs
                .findByApplication_IdAndDocType(applicationId, docType)
                .orElseGet(() -> ApplicationDocument.builder()
                        .application(application)
                        .docType(docType)
                        .build());

        // Re-upload: clean up the previous file before swapping pointers.
        String previousPath = doc.getFileUrl();
        if (previousPath != null && !previousPath.isBlank()) {
            storage.delete(previousPath);
        }

        String originalName = file.getOriginalFilename();
        doc.setFileUrl(stored.storedPath());
        doc.setFileName(originalName == null || originalName.isBlank() ? "upload" : originalName);
        doc.setMimeType(stored.detectedMimeType());
        doc.setSizeBytes(stored.size());
        doc.setUploadedAt(OffsetDateTime.now());
        doc.setVerified(false); // re-uploads reset verified
        ApplicationDocument saved = appDocs.save(doc);

        // If every required type now has a row, flip to DOCUMENTS_SUBMITTED.
        UUID propertyId = application.getUnit().getProperty().getId();
        Set<DocumentType> required = requiredDocs.findByProperty_Id(propertyId).stream()
                .map(com.habitat.api.entity.PropertyRequiredDocument::getDocType)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        Set<DocumentType> uploaded = new HashSet<>();
        for (ApplicationDocument existing : appDocs.findByApplication_Id(applicationId)) {
            uploaded.add(existing.getDocType());
        }
        if (!required.isEmpty() && uploaded.containsAll(required)
                && application.getStatus() == ApplicationStatus.AWAITING_DOCUMENTS) {
            com.habitat.api.service.statemachine.ApplicationStateMachine
                    .transition(application, ApplicationStatus.DOCUMENTS_SUBMITTED);
            log.info("application {} all required docs uploaded — DOCUMENTS_SUBMITTED",
                    applicationId);
        }

        return ApplicationDocumentResponse.from(saved);
    }

    private void requireOwnerOrThrow(Application application, UUID callerId) {
        if (!application.getTenant().getId().equals(callerId)) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
    }

    // ── Listing endpoints ─────────────────────────────────────────────

    /** Applications the caller submitted, newest first. */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listForTenant() {
        UUID me = security.requireUserId();
        return applications.findByTenant_IdOrderByCreatedAtDesc(me).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    /** Applications received against properties the caller manages, newest first. */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listInboundForLandlord() {
        UUID me = security.requireUserId();
        return applications.findByUnit_Property_Manager_IdOrderByCreatedAtDesc(me).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    /** Single application by id — visible to the tenant who owns it OR the property manager. */
    @Transactional(readOnly = true)
    public ApplicationResponse getById(UUID id) {
        UUID me = security.requireUserId();
        Application application = applications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.APPLICATION_NOT_FOUND));
        UUID tenantId = application.getTenant().getId();
        UUID managerId = application.getUnit().getProperty().getManager() == null
                ? null
                : application.getUnit().getProperty().getManager().getId();
        if (!me.equals(tenantId) && !me.equals(managerId)) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
        return ApplicationResponse.from(application);
    }

    // ── Landlord review ──────────────────────────────────────────────

    /**
     * Landlord transitions an application: APPROVE / REJECT / ON_HOLD.
     * Valid source statuses are SUBMITTED / DOCUMENTS_SUBMITTED /
     * UNDER_REVIEW / ON_HOLD — anything past APPROVED is terminal-ish
     * and can't be re-reviewed.
     */
    @Transactional
    public ApplicationResponse review(UUID applicationId, ReviewApplicationRequest req) {
        UUID me = security.requireUserId();
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.APPLICATION_NOT_FOUND));

        var manager = application.getUnit().getProperty().getManager();
        if (manager == null || !me.equals(manager.getId())) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }

        ApplicationStatus current = application.getStatus();
        boolean reviewable =
                current == ApplicationStatus.SUBMITTED
                        || current == ApplicationStatus.DOCUMENTS_SUBMITTED
                        || current == ApplicationStatus.UNDER_REVIEW
                        || current == ApplicationStatus.ON_HOLD;
        if (!reviewable) {
            throw new ConflictException(ErrorMessages.APPLICATION_NOT_REVIEWABLE);
        }

        ApplicationStatus next = switch (req.action()) {
            case APPROVE -> ApplicationStatus.APPROVED;
            case REJECT  -> ApplicationStatus.REJECTED;
            case ON_HOLD -> ApplicationStatus.ON_HOLD;
        };
        com.habitat.api.service.statemachine.ApplicationStateMachine.transition(application, next);
        application.setDecisionNote(req.note());
        application.setDecidedAt(OffsetDateTime.now());
        application.setDecidedBy(me);
        // ARCH-04: snapshot the reviewer's display name so the audit
        // trail survives a later hard-delete of the user account.
        application.setDecidedByName(users.findById(me)
                .map(ApplicationService::displayName)
                .orElse(null));

        // Approval fires an AFTER_COMMIT event; the InvoiceIssuanceListener
        // creates the invoice and bumps the application to INVOICE_SENT.
        // Decouples ApplicationService from InvoiceService (TECH_DEBT
        // ARCH-03). The tenant-side notification fires off a parallel
        // listener (ApplicationApprovedNotificationListener).
        switch (next) {
            case APPROVED -> events.publishEvent(
                    new com.habitat.api.event.ApplicationApprovedEvent(applicationId));
            case REJECTED -> events.publishEvent(
                    new com.habitat.api.event.ApplicationRejectedEvent(applicationId));
            case ON_HOLD -> events.publishEvent(
                    new com.habitat.api.event.ApplicationOnHoldEvent(applicationId));
            default -> { /* no event for other transitions in this flow */ }
        }

        log.info("application {} reviewed by {} → {}", applicationId, me, application.getStatus());
        return ApplicationResponse.from(application);
    }

    private static String displayName(User u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last  = u.getSurname() == null   ? "" : u.getSurname();
        String name  = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }
}
