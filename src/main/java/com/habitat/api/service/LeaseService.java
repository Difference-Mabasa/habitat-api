package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.lease.DeclineLeaseRequest;
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
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.LeaseRepository;
import com.habitat.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final LeaseRepository leases;
    private final SecurityUtils security;

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
                            .build();
                    Lease saved = leases.save(fresh);
                    application.setStatus(ApplicationStatus.LEASE_PENDING_SIGNATURES);
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
     * Record a signature from the caller (tenant or landlord). When
     * both signatures land the lease flips to SIGNED. The downstream
     * application status (COMPLETED + unit OCCUPIED) lands in slice 4.
     */
    @Transactional
    public LeaseResponse sign(UUID id, SignLeaseRequest req) {
        UUID me = security.requireUserId();
        Lease lease = leases.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.LEASE_NOT_FOUND));

        if (lease.getStatus() != LeaseStatus.PENDING_SIGNATURES) {
            throw new ConflictException(ErrorMessages.LEASE_NOT_SIGNABLE);
        }

        UUID tenantId = lease.getTenant().getId();
        UUID landlordId = lease.getLandlord().getId();

        OffsetDateTime now = OffsetDateTime.now();
        if (me.equals(tenantId)) {
            if (lease.getTenantSignedAt() != null) {
                throw new ConflictException(ErrorMessages.LEASE_ALREADY_SIGNED);
            }
            lease.setTenantSignedAt(now);
        } else if (me.equals(landlordId)) {
            if (lease.getLandlordSignedAt() != null) {
                throw new ConflictException(ErrorMessages.LEASE_ALREADY_SIGNED);
            }
            lease.setLandlordSignedAt(now);
        } else {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }

        // OTP is mocked — req kept on the signature for forward-compat
        // when the real e-IDAS hook lands.
        if (req != null) {
            log.debug("lease {} signed by {} (otp present={})",
                    lease.getLeaseRef(), me, req.otp() != null);
        }

        if (lease.getTenantSignedAt() != null && lease.getLandlordSignedAt() != null) {
            lease.setStatus(LeaseStatus.SIGNED);
            log.info("lease {} fully signed", lease.getLeaseRef());
        }

        return LeaseResponse.from(lease);
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
}
