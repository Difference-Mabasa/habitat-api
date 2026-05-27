package com.habitat.api.dto.viewing;

import com.habitat.api.entity.Viewing;
import com.habitat.api.enums.ViewingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wire shape for {@link Viewing}. Inlines the property + unit
 * identity the UI needs to render a card without a second fetch.
 */
public record ViewingResponse(
        UUID id,
        UUID unitId,
        String unitTitle,
        UUID propertyId,
        String propertyTitle,
        String propertyAddress,
        UUID tenantUserId,
        String tenantName,
        OffsetDateTime scheduledAt,
        ViewingStatus status,
        String notes,
        String decisionNote,
        OffsetDateTime decidedAt,
        UUID cancelledBy,
        OffsetDateTime createdAt
) {
    public static ViewingResponse from(Viewing v) {
        var unit = v.getUnit();
        var property = unit == null ? null : unit.getProperty();
        var tenant = v.getTenant();
        String address = property == null ? null
                : joinNonBlank(property.getSuburb(), property.getCity());
        String tenantName = tenant == null ? null
                : joinNonBlank(tenant.getFirstName(), tenant.getSurname());
        if (tenantName == null && tenant != null) tenantName = tenant.getEmail();
        return new ViewingResponse(
                v.getId(),
                unit == null ? null : unit.getId(),
                unit == null ? null : unit.getTitle(),
                property == null ? null : property.getId(),
                property == null ? null : property.getTitle(),
                address,
                tenant == null ? null : tenant.getId(),
                tenantName,
                v.getScheduledAt(),
                v.getStatus(),
                v.getNotes(),
                v.getDecisionNote(),
                v.getDecidedAt(),
                v.getCancelledBy(),
                v.getCreatedAt()
        );
    }

    private static String joinNonBlank(String a, String b) {
        boolean haveA = a != null && !a.isBlank();
        boolean haveB = b != null && !b.isBlank();
        if (haveA && haveB) return a + " " + b;
        if (haveA) return a;
        if (haveB) return b;
        return null;
    }
}
