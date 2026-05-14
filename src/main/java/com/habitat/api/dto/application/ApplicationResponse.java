package com.habitat.api.dto.application;

import com.habitat.api.entity.Application;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.EmploymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public payload returned after creating an application or fetching it.
 * Carries the parent unit + property context inline so list views don't
 * need a second round-trip to render a card.
 */
public record ApplicationResponse(
        UUID id,
        UUID unitId,
        UUID tenantId,
        ApplicationStatus status,
        String message,
        LocalDate moveInDate,
        EmploymentStatus employmentStatus,
        String decisionNote,
        OffsetDateTime decidedAt,
        OffsetDateTime createdAt,
        /** Inline parent-unit context for list-card rendering. */
        UnitRef unit,
        PropertyRef property,
        /** Tenant identity for the landlord-side applicant detail view. */
        TenantRef tenant
) {
    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
                a.getId(),
                a.getUnit().getId(),
                a.getTenant().getId(),
                a.getStatus(),
                a.getMessage(),
                a.getMoveInDate(),
                a.getEmploymentStatus(),
                a.getDecisionNote(),
                a.getDecidedAt(),
                a.getCreatedAt(),
                UnitRef.from(a.getUnit()),
                PropertyRef.from(a.getUnit().getProperty()),
                TenantRef.from(a.getTenant())
        );
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
        public static UnitRef from(com.habitat.api.entity.Unit u) {
            String cover = u.getImages().stream()
                    .filter(i -> Boolean.TRUE.equals(i.getIsCover()))
                    .map(com.habitat.api.entity.UnitImage::getUrl)
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
            String suburb,
            String city,
            String province
    ) {
        public static PropertyRef from(com.habitat.api.entity.Property p) {
            return new PropertyRef(p.getId(), p.getTitle(), p.getSuburb(), p.getCity(), p.getProvince());
        }
    }

    public record TenantRef(UUID id, String firstName, String surname, String email) {
        public static TenantRef from(com.habitat.api.entity.User u) {
            return new TenantRef(u.getId(), u.getFirstName(), u.getSurname(), u.getEmail());
        }
    }
}
