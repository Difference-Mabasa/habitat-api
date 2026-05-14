package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.landing.LandingStatsResponse;
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
    void stats_returns_three_counts() throws Exception {
        when(landing.stats()).thenReturn(new LandingStatsResponse(20L, 7L, 11L));

        mvc.perform(get(ApiRoutes.LANDING_STATS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeListings").value(20))
                .andExpect(jsonPath("$.registeredTenants").value(7))
                .andExpect(jsonPath("$.suburbsCovered").value(11));
    }

    @Test
    void stats_serializes_zeros_explicitly() throws Exception {
        when(landing.stats()).thenReturn(new LandingStatsResponse(0L, 0L, 0L));

        mvc.perform(get(ApiRoutes.LANDING_STATS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeListings").value(0))
                .andExpect(jsonPath("$.registeredTenants").value(0))
                .andExpect(jsonPath("$.suburbsCovered").value(0));
    }
}
