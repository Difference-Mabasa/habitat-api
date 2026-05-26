package com.habitat.api.dto.property;

import com.habitat.api.entity.Amenity;

import java.util.UUID;

/**
 * Canonical amenity row. Returned by {@code GET /amenities} and
 * nested on {@link PropertyDetailResponse}.
 *
 * <p>{@code icon} is a habitat-ui outline-icon name — drop straight
 * into {@code <Icon name=...>}.
 */
public record AmenityResponse(
        UUID id,
        String name,
        String icon,
        int sortOrder
) {
    public static AmenityResponse from(Amenity a) {
        return new AmenityResponse(a.getId(), a.getName(), a.getIcon(), a.getSortOrder());
    }
}
