package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.landing.LandingStatsResponse;
import com.habitat.api.dto.landing.PopularCityResponse;
import com.habitat.api.service.LandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public read-only aggregates for the marketing landing page. No auth
 * required — the trust strip is the first thing anonymous visitors see.
 * Permitted via {@code PublicEndpoints.PATHS}.
 */
@RestController
@RequestMapping(ApiRoutes.LANDING)
@RequiredArgsConstructor
public class LandingController {

    private final LandingService landing;

    @GetMapping("/stats")
    public LandingStatsResponse stats() {
        return landing.stats();
    }

    @GetMapping("/cities")
    public List<PopularCityResponse> cities(
            @RequestParam(defaultValue = "7") int size
    ) {
        return landing.popularCities(size);
    }
}
