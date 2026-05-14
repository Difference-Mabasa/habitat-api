package com.habitat.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against the seeded catalogue. Confirms the public
 * /landing/stats endpoint reports three sensible counts derived from
 * what V2 + V10..V12 actually inserted.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class LandingFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("habitat_test")
            .withUsername("habitat")
            .withPassword("habitat");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void stats_reports_real_counts_against_seeded_catalogue() throws Exception {
        String body = mvc.perform(get(ApiRoutes.LANDING_STATS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeListings").exists())
                .andExpect(jsonPath("$.registeredTenants").exists())
                .andExpect(jsonPath("$.suburbsCovered").exists())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);

        // V10..V12 seed 20+ LISTED properties across multiple suburbs;
        // V2 seeds demo users including a handful of USER role accounts.
        // We assert lower bounds + cross-stat consistency, not exact
        // numbers, so re-seeding doesn't force a churned test update.
        assertThat(root.get("activeListings").asLong()).isPositive();
        assertThat(root.get("registeredTenants").asLong()).isPositive();
        assertThat(root.get("suburbsCovered").asLong()).isPositive();

        // suburbsCovered is always <= activeListings (each listing
        // contributes at most one distinct suburb).
        assertThat(root.get("suburbsCovered").asLong())
                .isLessThanOrEqualTo(root.get("activeListings").asLong());
    }
}
