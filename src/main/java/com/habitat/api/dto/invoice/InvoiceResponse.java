package com.habitat.api.dto.invoice;

import com.habitat.api.entity.Invoice;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.UnitImage;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public payload for {@code GET /invoices/me} and {@code POST
 * /invoices/{id}/pay}. Carries the parent application + property /
 * unit context inline so the tenant pay-deposit screen can render
 * without a second round-trip.
 */
public record InvoiceResponse(
        UUID id,
        String invoiceRef,
        UUID applicationId,
        UUID tenantId,
        InvoiceStatus status,
        BigDecimal depositAmount,
        BigDecimal firstMonthRent,
        BigDecimal totalAmount,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime paidAt,
        String paymentReference,
        OffsetDateTime createdAt,
        ApplicationRef application,
        UnitRef unit,
        PropertyRef property
) {
    public static InvoiceResponse from(Invoice i) {
        // V22: read off the invoice's direct refs — application is a
        // nullable trace pointer only.
        var application = i.getApplication();
        var unit = i.getUnit();
        var property = i.getProperty();
        ApplicationRef appRef = application == null
                ? null
                : new ApplicationRef(application.getId(), application.getStatus());
        UUID appId = application == null ? null : application.getId();
        return new InvoiceResponse(
                i.getId(),
                i.getInvoiceRef(),
                appId,
                i.getTenant().getId(),
                i.getStatus(),
                i.getDepositAmount(),
                i.getFirstMonthRent(),
                i.getTotalAmount(),
                i.getIssuedAt(),
                i.getExpiresAt(),
                i.getPaidAt(),
                i.getPaymentReference(),
                i.getCreatedAt(),
                appRef,
                UnitRef.from(unit),
                new PropertyRef(
                        property.getId(),
                        property.getTitle(),
                        property.getSuburb(),
                        property.getCity(),
                        property.getProvince()
                )
        );
    }

    public record ApplicationRef(UUID id, ApplicationStatus status) {}

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
            String suburb,
            String city,
            String province
    ) {}
}
