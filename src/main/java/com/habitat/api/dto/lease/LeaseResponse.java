package com.habitat.api.dto.lease;

import com.habitat.api.entity.Lease;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.UnitImage;
import com.habitat.api.entity.User;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.LeaseStatus;
import com.habitat.api.enums.LeaseTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public payload for {@code GET /leases/me}, {@code GET /leases/{id}},
 * sign + decline. Reads its identity off the lease's direct refs —
 * the parent application is a nullable trace pointer only.
 */
public record LeaseResponse(
        UUID id,
        String leaseRef,
        /** Null after the parent application has been archived (V22 trace pointer). */
        UUID applicationId,
        LeaseStatus status,
        LeaseTemplate template,
        BigDecimal monthlyRent,
        BigDecimal deposit,
        Integer termMonths,
        LocalDate startDate,
        OffsetDateTime tenantSignedAt,
        OffsetDateTime landlordSignedAt,
        String declineReason,
        OffsetDateTime createdAt,
        /** Nullable — present while the application row still exists. */
        ApplicationRef application,
        PartyRef tenant,
        PartyRef landlord,
        UnitRef unit,
        PropertyRef property
) {
    public static LeaseResponse from(Lease l) {
        var application = l.getApplication();
        ApplicationRef appRef = application == null
                ? null
                : new ApplicationRef(application.getId(), application.getStatus());
        return new LeaseResponse(
                l.getId(),
                l.getLeaseRef(),
                application == null ? null : application.getId(),
                l.getStatus(),
                l.getTemplate(),
                l.getMonthlyRent(),
                l.getDeposit(),
                l.getTermMonths(),
                l.getStartDate(),
                l.getTenantSignedAt(),
                l.getLandlordSignedAt(),
                l.getDeclineReason(),
                l.getCreatedAt(),
                appRef,
                PartyRef.from(l.getTenant()),
                PartyRef.from(l.getLandlord()),
                UnitRef.from(l.getUnit()),
                PropertyRef.from(l.getProperty())
        );
    }

    public record ApplicationRef(UUID id, ApplicationStatus status) {}

    public record PartyRef(UUID id, String firstName, String surname, String email) {
        public static PartyRef from(User u) {
            if (u == null) return null;
            return new PartyRef(u.getId(), u.getFirstName(), u.getSurname(), u.getEmail());
        }
    }

    public record UnitRef(
            UUID id,
            String title,
            String unitNumber,
            BigDecimal price,
            Integer bedrooms,
            Integer bathrooms,
            String coverImageUrl
    ) {
        public static UnitRef from(Unit u) {
            String cover = u.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsCover()))
                    .map(UnitImage::getUrl)
                    .findFirst()
                    .orElseGet(() -> u.getImages().isEmpty() ? null : u.getImages().get(0).getUrl());
            return new UnitRef(
                    u.getId(),
                    u.getTitle(),
                    u.getUnitNumber(),
                    u.getPrice(),
                    u.getBedrooms(),
                    u.getBathrooms(),
                    cover
            );
        }
    }

    public record PropertyRef(
            UUID id,
            String title,
            String addressLine,
            String suburb,
            String city,
            String province,
            String postalCode
    ) {
        public static PropertyRef from(Property p) {
            return new PropertyRef(
                    p.getId(),
                    p.getTitle(),
                    p.getAddressLine(),
                    p.getSuburb(),
                    p.getCity(),
                    p.getProvince(),
                    p.getPostalCode()
            );
        }
    }
}
