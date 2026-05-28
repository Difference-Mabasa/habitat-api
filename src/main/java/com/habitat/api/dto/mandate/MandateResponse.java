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
        /** Agent's first name, snapshotted from the User row. Drives
         *  the inbox row "who's asking" line without forcing the UI
         *  to fetch the property per row to get manager identity. */
        String agentFirstName,
        /** Agent's surname, snapshotted. See {@link #agentFirstName}. */
        String agentSurname,
        /** Property title, snapshotted from the Property row. Lets
         *  /mandate-approvals show "what's this about" without
         *  fetching PropertyDetail per row. */
        String propertyTitle,
        /** Property suburb, snapshotted; nullable since
         *  {@code Property.suburb} is nullable. */
        String propertySuburb,
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
        OffsetDateTime createdAt,
        /** Typed name captured at online-flow approve time. Null for
         *  offline-flow approvals and pre-slice-2 ACTIVE rows. */
        String signedName,
        /** Server timestamp of the online approve. Paired with
         *  {@link #signedName}; null when null. */
        OffsetDateTime signedAt
) {
    public static MandateResponse from(Mandate m) {
        var property = m.getProperty();
        var landlord = property == null ? null : property.getLandlord();
        var agent = m.getAgent();
        return new MandateResponse(
                m.getId(),
                property == null ? null : property.getId(),
                agent == null ? null : agent.getId(),
                LandlordRef.from(landlord),
                agent == null ? null : agent.getFirstName(),
                agent == null ? null : agent.getSurname(),
                property == null ? null : property.getTitle(),
                property == null ? null : property.getSuburb(),
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
                m.getCreatedAt(),
                m.getSignedName(),
                m.getSignedAt()
        );
    }
}
