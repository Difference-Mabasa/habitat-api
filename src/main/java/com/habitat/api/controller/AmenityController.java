package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.property.AmenityResponse;
import com.habitat.api.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Canonical amenity list. Public — the landing-page filter, the browse
 * sidebar, and the listing wizard all read from here.
 */
@RestController
@RequestMapping(ApiRoutes.AMENITIES)
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenities;

    @GetMapping
    public List<AmenityResponse> list() {
        return amenities.listAll();
    }
}
