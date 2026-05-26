package com.habitat.api.dto.property;

import com.habitat.api.enums.ListingMode;
import com.habitat.api.enums.PropertyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for {@code POST /properties}. Optional {@code listingMode}
 * defaults to {@code LANDLORD_DIRECT} when omitted (matches the
 * wizard's default radio selection). {@code mandateFeePercent} is
 * only meaningful when {@code listingMode == AGENT_MANAGED}; the
 * service validates the pair.
 */
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
        Double longitude,
        ListingMode listingMode,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal mandateFeePercent
) {}
