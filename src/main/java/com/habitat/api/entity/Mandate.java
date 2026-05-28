package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * Agent-landlord mandate on an agent-managed property. Online
 * landlords approve in-app; offline landlords sign a PDF the agent
 * emails them. See {@link MandateStatus} for the state machine.
 *
 * <p>Landlord identity isn't carried directly here — it lives on
 * {@code property.landlord} (a {@link Landlord} row) which the mandate
 * issue flow resolves via {@code LandlordService.resolveForMandate}.
 * That keeps the same identity across multiple mandates and across
 * the offline → online flip.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mandates")
@SQLRestriction("deleted_at IS NULL")
public class Mandate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_user_id", nullable = false)
    private User agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "mandate_type", nullable = false, length = 40)
    private MandateType mandateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private MandateStatus status;

    @Column(name = "agent_attested", nullable = false)
    @Builder.Default
    private boolean agentAttested = false;

    /** Agent fee percent — mirrors Property.mandateFeePercent at issuance. */
    @Column(name = "fee_percent", precision = 5, scale = 2)
    private BigDecimal feePercent;

    /** Storage-relative path of the generated mandate PDF. */
    @Column(name = "mandate_document_path", length = 500)
    private String mandateDocumentPath;

    /** Storage-relative path of the signed PDF (offline flow). */
    @Column(name = "signed_document_path", length = 500)
    private String signedDocumentPath;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /** Typed name captured from the online landlord at approve time.
     *  Null for offline-flow approvals and pre-slice-2 ACTIVE rows. */
    @Column(name = "signed_name", length = 120)
    private String signedName;

    /** Server timestamp of the online approve. Paired with
     *  {@link #signedName}; null when null. */
    @Column(name = "signed_at")
    private OffsetDateTime signedAt;
}
