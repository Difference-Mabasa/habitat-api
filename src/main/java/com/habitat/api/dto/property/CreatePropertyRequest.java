package com.habitat.api.dto.property;

import com.habitat.api.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePropertyRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull PropertyType propertyType,
        @Size(max = 255) String addressLine,
        @Size(max = 120) String suburb,
        @Size(max = 120) String city,
        @Size(max = 80) String province,
        @Size(max = 10) String postalCode,
        Double latitude,
        Double longitude
) {}
