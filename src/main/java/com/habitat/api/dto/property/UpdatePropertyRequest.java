package com.habitat.api.dto.property;

import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.PropertyType;
import jakarta.validation.constraints.Size;

/**
 * PATCH-style update — every field optional. Null = "don't touch that
 * column"; empty string normalises to null at the service layer.
 */
public record UpdatePropertyRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        PropertyType propertyType,
        PropertyStatus status,
        @Size(max = 255) String addressLine,
        @Size(max = 120) String suburb,
        @Size(max = 120) String city,
        @Size(max = 80) String province,
        @Size(max = 10) String postalCode,
        Double latitude,
        Double longitude
) {}
