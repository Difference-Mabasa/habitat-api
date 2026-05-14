package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.landing.LandingStatsResponse;
import com.habitat.api.dto.landing.PopularCityResponse;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.LandingService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LandingController.class)
@AutoConfigureMockMvc(addFilters = false)
class LandingControllerTest {

    @Autowired MockMvc mvc;
    @MockBean LandingService landing;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    @Test
    void stats_returns_four_counts() throws Exception {
        when(landing.stats()).thenReturn(new LandingStatsResponse(20L, 7L, 11L, 3L));

        mvc.perform(get(ApiRoutes.LANDING_STATS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeListings").value(20))
                .andExpect(jsonPath("$.registeredTenants").value(7))
                .andExpect(jsonPath("$.suburbsCovered").value(11))
                .andExpect(jsonPath("$.tenantsLast7Days").value(3));
    }

    @Test
    void stats_serializes_zeros_explicitly() throws Exception {
        when(landing.stats()).thenReturn(new LandingStatsResponse(0L, 0L, 0L, 0L));

        mvc.perform(get(ApiRoutes.LANDING_STATS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeListings").value(0))
                .andExpect(jsonPath("$.registeredTenants").value(0))
                .andExpect(jsonPath("$.suburbsCovered").value(0))
                .andExpect(jsonPath("$.tenantsLast7Days").value(0));
    }

    @Test
    void cities_returns_payload_with_default_size() throws Exception {
        when(landing.popularCities(7)).thenReturn(List.of(
                new PopularCityResponse("Johannesburg", 12L),
                new PopularCityResponse("Cape Town", 5L)
        ));

        mvc.perform(get(ApiRoutes.LANDING_CITIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Johannesburg"))
                .andExpect(jsonPath("$[0].listingCount").value(12))
                .andExpect(jsonPath("$[1].name").value("Cape Town"));

        verify(landing).popularCities(7);
    }

    @Test
    void cities_forwards_size_query_param() throws Exception {
        when(landing.popularCities(3)).thenReturn(List.of());

        mvc.perform(get(ApiRoutes.LANDING_CITIES).param("size", "3"))
                .andExpect(status().isOk());

        verify(landing).popularCities(3);
    }
}
