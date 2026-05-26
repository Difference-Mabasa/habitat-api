package com.habitat.api.dto.mandate;

import com.habitat.api.entity.Mandate;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wire shape for {@link Mandate}. Online + offline landlord fields
 * both surface so the UI can render the matching state without
 * branching on type beyond {@code landlordUserId == null}.
 */
public record MandateResponse(
        UUID id,
        UUID propertyId,
        UUID agentUserId,
        UUID landlordUserId,
        String offlineLandlordName,
        String offlineLandlordEmail,
        String offlineLandlordPhone,
        MandateType mandateType,
        MandateStatus status,
        boolean agentAttested,
        BigDecimal feePercent,
        /** Path the agent can hit to download the generated PDF. */
        String mandateDownloadUrl,
        /** Path the agent can hit to download the signed PDF, if uploaded. */
        String signedDownloadUrl,
        String notes,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {
    public static MandateResponse from(Mandate m) {
        return new MandateResponse(
                m.getId(),
                m.getProperty() == null ? null : m.getProperty().getId(),
                m.getAgent() == null ? null : m.getAgent().getId(),
                m.getLandlordUser() == null ? null : m.getLandlordUser().getId(),
                m.getOfflineLandlordName(),
                m.getOfflineLandlordEmail(),
                m.getOfflineLandlordPhone(),
                m.getMandateType(),
                m.getStatus(),
                m.isAgentAttested(),
                m.getFeePercent(),
                m.getMandateDocumentPath() == null
                        ? null
                        : "/api/v1/properties/" + (m.getProperty() == null ? "" : m.getProperty().getId()) + "/mandate/pdf",
                m.getSignedDocumentPath() == null
                        ? null
                        : "/api/v1/properties/" + (m.getProperty() == null ? "" : m.getProperty().getId()) + "/mandate/signed",
                m.getNotes(),
                m.getExpiresAt(),
                m.getCreatedAt()
        );
    }
}
