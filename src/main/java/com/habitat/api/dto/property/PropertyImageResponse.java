package com.habitat.api.dto.property;

import com.habitat.api.entity.PropertyImage;

import java.util.UUID;

public record PropertyImageResponse(
        UUID id,
        /** Browser-loadable URL — fully qualified for legacy seed rows
         *  (Unsplash) or routed through {@code /api/v1/files/images/}
         *  for rows written by the upload endpoint. */
        String url,
        boolean isCover,
        int sortOrder
) {
    private static final String IMAGE_BASE = "/api/v1/files/images/";

    public static PropertyImageResponse from(PropertyImage img) {
        return new PropertyImageResponse(
                img.getId(),
                toPublicUrl(img.getUrl()),
                Boolean.TRUE.equals(img.getIsCover()),
                img.getSortOrder() == null ? 0 : img.getSortOrder()
        );
    }

    static String toPublicUrl(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        // Legacy seed rows already carry a full URL.
        if (stored.startsWith("http://") || stored.startsWith("https://")) return stored;
        return IMAGE_BASE + stored;
    }
}
