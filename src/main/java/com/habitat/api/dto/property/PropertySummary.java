package com.habitat.api.dto.property;

import com.habitat.api.entity.Property;
import com.habitat.api.entity.PropertyImage;
import com.habitat.api.entity.Unit;
import com.habitat.api.enums.PropertyType;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.UUID;

/**
 * Slim per-row payload for the public {@code GET /properties} list. Picks
 * the cheapest available unit as the "headline" so /browse can render a
 * single PropertyCard per property with a defensible price/beds tag.
 */
public record PropertySummary(
        UUID id,
        String title,
        String suburb,
        String city,
        String province,
        Double latitude,
        Double longitude,
        PropertyType propertyType,
        String coverImageUrl,
        UUID headlineUnitId,
        UnitType headlineUnitType,
        BigDecimal headlinePrice,
        Integer headlineBeds,
        Integer headlineBaths,
        Integer headlineSqm,
        int totalUnits,
        int availableUnits,
        /**
         * When the property was first created. Surfaced so callers that
         * sort by recency (the landing "Just listed" card, for example)
         * can render a relative-time subline without a second fetch.
         */
        OffsetDateTime createdAt,
        /**
         * Aggregated rating from reviews (0.00–5.00). Recomputed
         * out-of-band by a future review service; zero by default for
         * new / un-reviewed properties.
         */
        BigDecimal avgRating,
        /** Number of reviews backing {@code avgRating}; drives the "(N)" count next to stars. */
        int ratingCount
) {
    public static PropertySummary from(Property p) {
        Unit head = p.getUnits().stream()
                .filter(u -> u.getStatus() == UnitStatus.AVAILABLE)
                .min(Comparator.comparing(Unit::getPrice))
                .orElse(null);
        String cover = p.getImages().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsCover()))
                .map(PropertyImage::getUrl)
                .findFirst()
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl());
        int available = (int) p.getUnits().stream()
                .filter(u -> u.getStatus() == UnitStatus.AVAILABLE)
                .count();
        return new PropertySummary(
                p.getId(),
                p.getTitle(),
                p.getSuburb(),
                p.getCity(),
                p.getProvince(),
                p.getLatitude(),
                p.getLongitude(),
                p.getPropertyType(),
                cover,
                head == null ? null : head.getId(),
                head == null ? null : head.getUnitType(),
                head == null ? null : head.getPrice(),
                head == null ? null : head.getBedrooms(),
                head == null ? null : head.getBathrooms(),
                head == null ? null : head.getSqm(),
                p.getUnits().size(),
                available,
                p.getCreatedAt(),
                p.getAvgRating(),
                p.getRatingCount()
        );
    }
}
