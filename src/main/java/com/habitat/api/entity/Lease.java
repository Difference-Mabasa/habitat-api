package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.LeaseStatus;
import com.habitat.api.enums.LeaseTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Residential lease agreement between landlord and tenant. Generated
 * the moment the tenant pays the deposit invoice. Both parties sign
 * (mocked OTP today) to advance the parent application to COMPLETED.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "leases")
@SQLRestriction("deleted_at IS NULL")
public class Lease extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(name = "template", nullable = false, length = 40)
    @Builder.Default
    private LeaseTemplate template = LeaseTemplate.RHA_STANDARD;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "deposit", nullable = false, precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(name = "term_months", nullable = false)
    @Builder.Default
    private Integer termMonths = 12;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private LeaseStatus status = LeaseStatus.PENDING_SIGNATURES;

    /** Human-readable reference. Pattern: HB-LSE-XXXXXXXX. */
    @Column(name = "lease_ref", nullable = false, length = 40, unique = true)
    private String leaseRef;

    @Column(name = "tenant_signed_at")
    private OffsetDateTime tenantSignedAt;

    @Column(name = "landlord_signed_at")
    private OffsetDateTime landlordSignedAt;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "signed_pdf_url", length = 500)
    private String signedPdfUrl;
}
