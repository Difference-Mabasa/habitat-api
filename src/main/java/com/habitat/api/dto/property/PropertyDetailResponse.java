package com.habitat.api.dto.property;

import com.habitat.api.entity.Property;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.PropertyType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full property payload returned by GET /properties/{id} and write
 * endpoints. Includes nested units + photos so the detail screen and
 * write callers don't need a second round-trip.
 */
public record PropertyDetailResponse(
        UUID id,
        String title,
        String description,
        PropertyType propertyType,
        PropertyStatus status,
        String addressLine,
        String suburb,
        String city,
        String province,
        String postalCode,
        Double latitude,
        Double longitude,
        UUID landlordId,
        UUID managerId,
        /** Minimal manager identity for the "Listed by" card; null when no manager set. */
        ManagerRef manager,
        List<PropertyImageResponse> images,
        List<UnitResponse> units,
        OffsetDateTime createdAt,
        /** Aggregated rating (0.00–5.00). Zero for un-reviewed listings. */
        BigDecimal avgRating,
        /** Number of reviews backing avgRating. Zero for un-reviewed listings. */
        int ratingCount
) {
    public static PropertyDetailResponse from(Property p) {
        return new PropertyDetailResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getPropertyType(),
                p.getStatus(),
                p.getAddressLine(),
                p.getSuburb(),
                p.getCity(),
                p.getProvince(),
                p.getPostalCode(),
                p.getLatitude(),
                p.getLongitude(),
                p.getLandlord() == null ? null : p.getLandlord().getId(),
                p.getManager() == null ? null : p.getManager().getId(),
                ManagerRef.from(p.getManager()),
                p.getImages().stream().map(PropertyImageResponse::from).toList(),
                p.getUnits().stream().map(UnitResponse::from).toList(),
                p.getCreatedAt(),
                p.getAvgRating(),
                p.getRatingCount()
        );
    }
}
