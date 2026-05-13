package com.habitat.api.dto.property;

import com.habitat.api.entity.PropertyImage;

import java.util.UUID;

public record PropertyImageResponse(
        UUID id,
        String url,
        boolean isCover,
        int sortOrder
) {
    public static PropertyImageResponse from(PropertyImage img) {
        return new PropertyImageResponse(
                img.getId(),
                img.getUrl(),
                Boolean.TRUE.equals(img.getIsCover()),
                img.getSortOrder() == null ? 0 : img.getSortOrder()
        );
    }
}
