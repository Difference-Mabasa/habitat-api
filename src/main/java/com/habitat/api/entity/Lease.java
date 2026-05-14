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
import jakarta.persistence.ManyToOne;
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
 * the moment the tenant pays the deposit invoice.
 *
 * <p><b>Identity is independent of the parent application</b> (V22):
 * the lease carries its own {@link #tenant}, {@link #landlord},
 * {@link #unit}, and {@link #property} references. {@link #application}
 * is a nullable trace pointer — if the application row is ever archived
 * or hard-deleted, the lease still resolves to all parties and the
 * unit. FKs to users / units / properties are {@code ON DELETE
 * RESTRICT} at the schema level.
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

    /** Nullable trace pointer — kept for "which application generated this lease" lineage. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", unique = true)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

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
