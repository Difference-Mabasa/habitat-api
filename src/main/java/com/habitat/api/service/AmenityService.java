package com.habitat.api.service;

import com.habitat.api.dto.property.AmenityResponse;
import com.habitat.api.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-side accessor for the canonical amenity catalogue. Mostly a
 * thin shim over {@link AmenityRepository} — it exists so
 * {@link com.habitat.api.controller.AmenityController} stays inside
 * the {@code controllers → services → repositories} ArchUnit rule.
 */
@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenities;

    @Transactional(readOnly = true)
    public List<AmenityResponse> listAll() {
        return amenities.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(AmenityResponse::from)
                .toList();
    }
}
