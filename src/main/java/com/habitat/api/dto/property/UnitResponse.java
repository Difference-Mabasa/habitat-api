package com.habitat.api.dto.property;

import com.habitat.api.entity.Unit;
import com.habitat.api.enums.FurnishingType;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UnitResponse(
        UUID id,
        UUID propertyId,
        String unitNumber,
        String title,
        String description,
        UnitType unitType,
        UnitStatus status,
        FurnishingType furnishing,
        BigDecimal price,
        PaymentFrequency paymentFrequency,
        BigDecimal deposit,
        int bedrooms,
        int bathrooms,
        Integer sqm,
        Integer maxOccupants,
        boolean waterIncluded,
        boolean electricityIncluded,
        boolean petsAllowed,
        LocalDate availableFrom,
        List<UnitImageResponse> images
) {
    public static UnitResponse from(Unit u) {
        return new UnitResponse(
                u.getId(),
                u.getProperty() == null ? null : u.getProperty().getId(),
                u.getUnitNumber(),
                u.getTitle(),
                u.getDescription(),
                u.getUnitType(),
                u.getStatus(),
                u.getFurnishing(),
                u.getPrice(),
                u.getPaymentFrequency(),
                u.getDeposit(),
                u.getBedrooms() == null ? 0 : u.getBedrooms(),
                u.getBathrooms() == null ? 0 : u.getBathrooms(),
                u.getSqm(),
                u.getMaxOccupants(),
                Boolean.TRUE.equals(u.getWaterIncluded()),
                Boolean.TRUE.equals(u.getElectricityIncluded()),
                Boolean.TRUE.equals(u.getPetsAllowed()),
                u.getAvailableFrom(),
                u.getImages().stream().map(UnitImageResponse::from).toList()
        );
    }
}
