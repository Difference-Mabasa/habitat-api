package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.constants.StorageConstants;
import com.habitat.api.dto.lease.DeclineLeaseRequest;
import com.habitat.api.dto.lease.IssueOtpResponse;
import com.habitat.api.dto.lease.LeaseResponse;
import com.habitat.api.dto.lease.SignLeaseRequest;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.Lease;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.LeaseStatus;
import com.habitat.api.enums.LeaseTemplate;
import com.habitat.api.event.LeaseSignedEvent;
import com.habitat.api.exception.BadRequestException;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.LeaseRepository;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.storage.StorageService;
import com.habitat.api.storage.StoredFile;
import com.habitat.api.storage.StoredResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lease lifecycle. Auto-generated when a tenant pays the deposit
 * invoice; signed via mock OTP by tenant + landlord. Once both
 * signatures are present the parent application advances toward
 * COMPLETED (slice 4 closes that final step).
 *
 * <p>Reads + auth checks pull off the lease's direct party refs
 * ({@code tenant}, {@code landlord}, {@code unit}, {@code property})
 * — the parent application is a nullable trace pointer only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseService {

    private static final String OTP_PURPOSE_LEASE_SIGN = "lease-sign";

    private final LeaseRepository leases;
    private final ApplicationEventPublisher events;
    private final SecurityUtils security;
    private final OtpService otp;
    private final LeasePdfService leasePdf;
    private final StorageService storage;

    /**
     * Idempotently issue a lease for an application whose deposit has
     * just been paid. Re-paying or flapping doesn't duplicate the row.
     * Snapshots the party graph at issuance: even if the application
     * is later archived the lease still resolves to tenant + landlord
     * + unit + property.
     */
    @Transactional
    public Lease issueForPaidApplication(Application application) {
        return leases.findByApplication_Id(application.getId())
                .orElseGet(() -> {
                    Unit unit = application.getUnit();
                    Property property = unit.getProperty();
                    User tenant = application.getTenant();
                    User landlord = property.getManager();
                    BigDecimal monthly = unit.getPrice();
                    BigDecimal deposit = monthly == null ? BigDecimal.ZERO : monthly;
                    LocalDate start = application.getMoveInDate() == null
                            ? LocalDate.now().plusDays(7)
                            : application.getMoveInDate();
                    Lease fresh = Lease.builder()
                            .application(application)
                            .tenant(tenant)
                            .landlord(landlord)
                            .unit(unit)
                            .property(property)
                            .template(LeaseTemplate.RHA_STANDARD)
                            .monthlyRent(monthly == null ? BigDecimal.ZERO : monthly)
                            .deposit(deposit)
                            .termMonths(12)
                            .startDate(start)
                            .status(LeaseStatus.PENDING_SIGNATURES)
                            .leaseRef(nextLeaseRef())
                            // BUG-02: snapshot the live values so the
                            // lease stays correct after upstream edits.
                            .tenantNameSnapshot(displayName(tenant))
                            .landlordNameSnapshot(displayName(landlord))
                            .unitTitleSnapshot(unit.getTitle())
                            .propertyTitleSnapshot(property.getTitle())
                            .propertyAddressSnapshot(formatAddress(property))
                            .build();
                    Lease saved = leases.save(fresh);
                    com.habitat.api.service.statemachine.ApplicationStateMachine
                            .transition(application, ApplicationStatus.LEASE_PENDING_SIGNATURES);
                    log.info("lease {} generated for application {} (start={}, monthly={})",
                            saved.getLeaseRef(), application.getId(), start, monthly);
                    return saved;
                });
    }

    /** Caller's leases, newest first. */
    @Transactional(readOnly = true)
    public List<LeaseResponse> listForTenant() {
        UUID me = security.requireUserId();
        return leases.findByTenant_IdOrderByCreatedAtDesc(me).stream()
                .map(LeaseResponse::from)
                .toList();
    }

    /** Leases on properties the caller manages, newest first. */
    @Transactional(readOnly = true)
    public List<LeaseResponse> listForLandlord() {
        UUID me = security.requireUserId();
        return leases.findByLandlord_IdOrderByCreatedAtDesc(me).stream()
                .map(LeaseResponse::from)
                .toList();
    }

    /** Single lease — visible to either party. */
    @Transactional(readOnly = true)
    public LeaseResponse getById(UUID id) {
        UUID me = security.requireUserId();
        Lease lease = leases.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.LEASE_NOT_FOUND));
        requirePartyOrThrow(lease, me);
        return LeaseResponse.from(lease);
    }

    /**
     * Issue a fresh sign OTP for the caller against this lease. The
     * code lives in Redis with a 5-minute TTL; any prior code for the
     * same (user, lease-sign) pair is overwritten. Returned inline as
     * {@code devCode} until email delivery (Phase 8) is wired.
     */
    @Transactional(readOnly = true)
    public IssueOtpResponse issueSignOtp(UUID leaseId) {
        UUID me = security.requireUserId();
        Lease lease = leases.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.LEASE_NOT_FOUND));
        requirePartyOrThrow(lease, me);
        if (lease.getStatus() != LeaseStatus.PENDING_SIGNATURES) {
            throw new ConflictException(ErrorMessages.LEASE_NOT_SIGNABLE);
        }
        String code = otp.issue(otpSubject(me, leaseId), OTP_PURPOSE_LEASE_SIGN);
        log.info("issued lease-sign OTP for user {} on lease {}", me, lease.getLeaseRef());
        return new IssueOtpResponse(code);
    }

    /**
     * Record a signature from the caller (tenant or landlord). When
     * both signatures land the lease flips to SIGNED, the signed PDF
     * is rendered and persisted, and a LeaseSignedEvent fires so the
     * move-in listener can finalise.
     */
    @Transactional
    public LeaseResponse sign(UUID id, SignLeaseRequest req) {
        UUID me = security.requireUserId();
        if (req == null || req.otp() == null || req.otp().isBlank()) {
            throw new BadRequestException(ErrorMessages.LEASE_OTP_INVALID);
        }
        Lease lease = leases.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.LEASE_NOT_FOUND));

        if (lease.getStatus() != LeaseStatus.PENDING_SIGNATURES) {
            throw new ConflictException(ErrorMessages.LEASE_NOT_SIGNABLE);
        }

        UUID tenantId = lease.getTenant().getId();
        UUID landlordId = lease.getLandlord().getId();

        if (!me.equals(tenantId) && !me.equals(landlordId)) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
        // Verify BEFORE flipping any state — failing OTP returns 400
        // without recording a signature timestamp.
        if (!otp.verifyAndConsume(otpSubject(me, id), OTP_PURPOSE_LEASE_SIGN, req.otp())) {
            throw new BadRequestException(ErrorMessages.LEASE_OTP_INVALID);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (me.equals(tenantId)) {
            if (lease.getTenantSignedAt() != null) {
                throw new ConflictException(ErrorMessages.LEASE_ALREADY_SIGNED);
            }
            lease.setTenantSignedAt(now);
        } else {
            if (lease.getLandlordSignedAt() != null) {
                throw new ConflictException(ErrorMessages.LEASE_ALREADY_SIGNED);
            }
            lease.setLandlordSignedAt(now);
        }
        log.info("lease {} signed by {}", lease.getLeaseRef(), me);

        if (lease.getTenantSignedAt() != null && lease.getLandlordSignedAt() != null) {
            lease.setStatus(LeaseStatus.SIGNED);
            // Render + persist the PDF before the listener fires. If
            // either render or storage throws, the whole sign rolls back
            // — better to ask the user to retry than to flip status with
            // no PDF backing it.
            byte[] pdf = leasePdf.render(lease);
            StoredFile stored = storage.storeTrustedBytes(
                    StorageConstants.FOLDER_LEASES,
                    lease.getLeaseRef() + ".pdf",
                    "application/pdf",
                    pdf);
            lease.setSignedPdfUrl(stored.storedPath());
            log.info("lease {} fully signed; PDF stored at {}",
                    lease.getLeaseRef(), stored.storedPath());
            // AFTER_COMMIT listener handles the move-in side effects:
            // application → COMPLETED, unit → OCCUPIED, party notifications.
            // Decouples slice-4 concerns from this service per
            // habitat-api/TECH_DEBT.md ARCH-03.
            events.publishEvent(new LeaseSignedEvent(lease.getId()));
        }

        return LeaseResponse.from(lease);
    }

    /**
     * Open the signed PDF for download. Only available after both
     * parties have signed; either party can fetch it.
     */
    @Transactional(readOnly = true)
    public PdfHandle openSignedPdf(UUID id) {
        UUID me = security.requireUserId();
        Lease lease = leases.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.LEASE_NOT_FOUND));
        requirePartyOrThrow(lease, me);
        String storedPath = lease.getSignedPdfUrl();
        if (storedPath == null || storedPath.isBlank()) {
            throw new ConflictException(ErrorMessages.LEASE_PDF_NOT_READY);
        }
        StoredResource resource = storage.open(storedPath);
        String filename = (lease.getLeaseRef() == null ? "lease" : lease.getLeaseRef()) + ".pdf";
        return new PdfHandle(resource, filename);
    }

    public record PdfHandle(StoredResource resource, String fileName) {}

    /**
     * Derive a stable subject UUID from (userId, leaseId) so a code
     * issued for one lease can't be replayed against a different lease
     * the same user is also signing.
     */
    private static UUID otpSubject(UUID userId, UUID leaseId) {
        return UUID.nameUUIDFromBytes(
                (userId + ":" + leaseId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Either party declines. Lease is terminal; the application can be withdrawn separately. */
    @Transactional
    public LeaseResponse decline(UUID id, DeclineLeaseRequest req) {
        UUID me = security.requireUserId();
        Lease lease = leases.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.LEASE_NOT_FOUND));
        requirePartyOrThrow(lease, me);

        if (lease.getStatus() != LeaseStatus.PENDING_SIGNATURES) {
            throw new ConflictException(ErrorMessages.LEASE_NOT_SIGNABLE);
        }
        lease.setStatus(LeaseStatus.DECLINED);
        lease.setDeclineReason(req == null ? null : req.reason());
        log.info("lease {} declined by {}", lease.getLeaseRef(), me);
        return LeaseResponse.from(lease);
    }

    private void requirePartyOrThrow(Lease lease, UUID callerId) {
        UUID tenantId = lease.getTenant().getId();
        UUID landlordId = lease.getLandlord().getId();
        if (!callerId.equals(tenantId) && !callerId.equals(landlordId)) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
    }

    private static String nextLeaseRef() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "HB-LSE-" + suffix;
    }

    private static String displayName(User u) {
        if (u == null) return null;
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }

    private static String formatAddress(Property p) {
        if (p == null) return null;
        return java.util.stream.Stream.of(
                        p.getAddressLine(), p.getSuburb(), p.getCity(), p.getPostalCode())
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }
}
