package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.InvoiceStatus;
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
import java.time.OffsetDateTime;

/**
 * Deposit + first-month invoice generated when a landlord approves an
 * application.
 *
 * <p><b>Identity is independent of the parent application</b> (V22):
 * the invoice carries its own {@link #tenant}, {@link #landlord},
 * {@link #unit}, and {@link #property} references. {@link #application}
 * is a nullable trace pointer — accounting records survive their
 * upstream application being archived or hard-deleted, and FKs to
 * users / units / properties are {@code ON DELETE RESTRICT}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoices")
@SQLRestriction("deleted_at IS NULL")
public class Invoice extends BaseEntity {

    /** Nullable trace pointer — kept for "which application this invoice billed". */
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

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "first_month_rent", precision = 12, scale = 2)
    private BigDecimal firstMonthRent;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    /** Human-readable invoice reference. Pattern: HB-INV-XXXXXX. */
    @Column(name = "invoice_ref", nullable = false, length = 40, unique = true)
    private String invoiceRef;

    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private OffsetDateTime issuedAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    /** Reference from the payment gateway — mocked today, real once Ozow lands. */
    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    // ── BUG-02 snapshots — frozen at issuance, never updated ────────────

    @Column(name = "tenant_name_snapshot", length = 200)
    private String tenantNameSnapshot;

    @Column(name = "property_title_snapshot", length = 200)
    private String propertyTitleSnapshot;

    @Column(name = "property_address_snapshot", columnDefinition = "TEXT")
    private String propertyAddressSnapshot;
}
