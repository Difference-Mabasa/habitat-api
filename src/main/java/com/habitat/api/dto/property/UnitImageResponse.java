package com.habitat.api.dto.property;

import com.habitat.api.entity.UnitImage;

import java.util.UUID;

public record UnitImageResponse(
        UUID id,
        String url,
        boolean isCover,
        int sortOrder
) {
    public static UnitImageResponse from(UnitImage img) {
        return new UnitImageResponse(
                img.getId(),
                PropertyImageResponse.toPublicUrl(img.getUrl()),
                Boolean.TRUE.equals(img.getIsCover()),
                img.getSortOrder() == null ? 0 : img.getSortOrder()
        );
    }
}
