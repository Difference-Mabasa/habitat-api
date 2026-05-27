package com.habitat.api.dto.mandate;

import com.habitat.api.dto.landlord.LandlordRef;
import com.habitat.api.entity.Mandate;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wire shape for {@link Mandate}. Landlord identity is surfaced via
 * {@code landlord} (a {@link LandlordRef} pulled from the property's
 * landlord row) so the UI can render online vs offline without
 * branching on null fields — the {@code landlord.type} discriminator
 * does the job.
 */
public record MandateResponse(
        UUID id,
        UUID propertyId,
        UUID agentUserId,
        /** Resolved landlord identity for this mandate's property.
         *  Null is unexpected here — every mandate-issued property
         *  has a landlord — but kept defensive. */
        LandlordRef landlord,
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
        var property = m.getProperty();
        var landlord = property == null ? null : property.getLandlord();
        return new MandateResponse(
                m.getId(),
                property == null ? null : property.getId(),
                m.getAgent() == null ? null : m.getAgent().getId(),
                LandlordRef.from(landlord),
                m.getMandateType(),
                m.getStatus(),
                m.isAgentAttested(),
                m.getFeePercent(),
                m.getMandateDocumentPath() == null
                        ? null
                        : "/api/v1/properties/" + (property == null ? "" : property.getId()) + "/mandate/pdf",
                m.getSignedDocumentPath() == null
                        ? null
                        : "/api/v1/properties/" + (property == null ? "" : property.getId()) + "/mandate/signed",
                m.getNotes(),
                m.getExpiresAt(),
                m.getCreatedAt()
        );
    }
}
