package com.habitat.api.dto.property;

import com.habitat.api.entity.Property;
import com.habitat.api.enums.DocumentType;
import com.habitat.api.enums.ListingMode;
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
        int ratingCount,
        /** Wizard "Mandate" step: who controls the listing. */
        ListingMode listingMode,
        /** Wizard "Mandate" step: agent fee percent. Null for LANDLORD_DIRECT. */
        BigDecimal mandateFeePercent,
        /** Property-level amenities (WiFi / Parking / Garden …). */
        List<AmenityResponse> amenities,
        /**
         * Document types tenants must submit when applying. Powers the
         * wizard's "Application requirements" step on draft resume.
         */
        List<DocumentType> requiredDocuments
) {
    public static PropertyDetailResponse from(Property p) {
        return fromWithRequiredDocs(p, List.of());
    }

    /**
     * Variant including the required-documents set. {@code PropertyService}
     * uses this on read so the wizard's edit-mode hydration doesn't
     * need a second round-trip.
     */
    public static PropertyDetailResponse fromWithRequiredDocs(
            Property p, List<DocumentType> requiredDocuments) {
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
                p.getRatingCount(),
                p.getListingMode(),
                p.getMandateFeePercent(),
                p.getAmenities().stream()
                        .sorted(java.util.Comparator
                                .comparingInt(com.habitat.api.entity.Amenity::getSortOrder)
                                .thenComparing(com.habitat.api.entity.Amenity::getName))
                        .map(AmenityResponse::from)
                        .toList(),
                requiredDocuments == null ? List.of() : requiredDocuments
        );
    }
}
