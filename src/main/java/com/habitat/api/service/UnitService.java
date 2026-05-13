package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdateUnitRequest;
import com.habitat.api.entity.Unit;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Per-unit edits route through here so the manager/landlord guard
 * (sourced from the property they belong to) is consistent with
 * {@link PropertyService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnitService {

    private final UnitRepository units;
    private final PropertyService properties;
    private final SecurityUtils security;

    @Transactional
    public UnitResponse update(UUID id, UpdateUnitRequest req) {
        Unit u = findOrThrow(id);
        properties.requireCanEdit(u.getProperty());

        if (req.unitNumber() != null)         u.setUnitNumber(blankToNull(req.unitNumber()));
        if (req.title() != null)              u.setTitle(req.title().trim());
        if (req.description() != null)        u.setDescription(blankToNull(req.description()));
        if (req.unitType() != null)           u.setUnitType(req.unitType());
        if (req.status() != null)             u.setStatus(req.status());
        if (req.furnishing() != null)         u.setFurnishing(req.furnishing());
        if (req.price() != null)              u.setPrice(req.price());
        if (req.paymentFrequency() != null)   u.setPaymentFrequency(req.paymentFrequency());
        if (req.deposit() != null)            u.setDeposit(req.deposit());
        if (req.bedrooms() != null)           u.setBedrooms(req.bedrooms());
        if (req.bathrooms() != null)          u.setBathrooms(req.bathrooms());
        if (req.sqm() != null)                u.setSqm(req.sqm());
        if (req.maxOccupants() != null)       u.setMaxOccupants(req.maxOccupants());
        if (req.waterIncluded() != null)      u.setWaterIncluded(req.waterIncluded());
        if (req.electricityIncluded() != null) u.setElectricityIncluded(req.electricityIncluded());
        if (req.petsAllowed() != null)        u.setPetsAllowed(req.petsAllowed());
        if (req.availableFrom() != null)      u.setAvailableFrom(req.availableFrom());

        log.info("unit {} updated by user {}", u.getId(), security.requireUserId());
        return UnitResponse.from(u);
    }

    @Transactional
    public void delete(UUID id) {
        Unit u = findOrThrow(id);
        properties.requireCanEdit(u.getProperty());
        u.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        log.info("unit {} deleted by user {}", u.getId(), security.requireUserId());
    }

    private Unit findOrThrow(UUID id) {
        return units.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.UNIT_NOT_FOUND));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
