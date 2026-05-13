package com.habitat.api.dto.property;

import com.habitat.api.enums.FurnishingType;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.UnitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateUnitRequest(
        @Size(max = 40) String unitNumber,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull UnitType unitType,
        FurnishingType furnishing,
        @NotNull @DecimalMin("0") BigDecimal price,
        PaymentFrequency paymentFrequency,
        @DecimalMin("0") BigDecimal deposit,
        @NotNull @Min(0) Integer bedrooms,
        @NotNull @Min(0) Integer bathrooms,
        @Min(0) Integer sqm,
        @Min(1) Integer maxOccupants,
        Boolean waterIncluded,
        Boolean electricityIncluded,
        Boolean petsAllowed,
        LocalDate availableFrom
) {}
