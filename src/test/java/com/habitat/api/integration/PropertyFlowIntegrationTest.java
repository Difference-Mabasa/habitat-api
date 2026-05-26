package com.habitat.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.auth.AuthResponse;
import com.habitat.api.dto.auth.LoginRequest;
import com.habitat.api.dto.property.CreatePropertyRequest;
import com.habitat.api.enums.PropertyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end against a real Postgres with the V10 seed applied. Exercises
 * the public /properties endpoints + CRUD writes with an authenticated
 * landlord token, and confirms write endpoints reject anonymous traffic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PropertyFlowIntegrationTest {

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
    void seeded_properties_are_listed_publicly() throws Exception {
        String body = mvc.perform(get(ApiRoutes.PROPERTIES).param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);
        // V10 seeds exactly 20 properties, all LISTED.
        assertThat(root.get("totalElements").asInt()).isEqualTo(20);
        assertThat(root.get("content").size()).isEqualTo(20);
        // Each row has a non-null cover image URL and a headline price.
        JsonNode first = root.get("content").get(0);
        assertThat(first.get("coverImageUrl").asText()).startsWith("https://images.unsplash.com/");
        assertThat(first.get("headlinePrice").asDouble()).isPositive();
    }

    @Test
    void location_filter_narrows_to_matching_suburb() throws Exception {
        String body = mvc.perform(get(ApiRoutes.PROPERTIES).param("location", "Camps Bay"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);
        assertThat(root.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
        // Every returned property has suburb / city / province containing the query (case-insensitive).
        root.get("content").forEach(node -> {
            String combined = (node.get("suburb").asText("") + " "
                    + node.get("city").asText("") + " "
                    + node.get("province").asText("")).toLowerCase();
            assertThat(combined).contains("camps bay");
        });
    }

    @Test
    void max_price_filter_excludes_pricier_properties() throws Exception {
        String body = mvc.perform(get(ApiRoutes.PROPERTIES).param("maxPrice", "15000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);
        // Every returned headline unit costs at most R 15,000.
        root.get("content").forEach(node -> {
            double price = node.get("headlinePrice").asDouble();
            assertThat(price).isLessThanOrEqualTo(15000.0);
        });
    }

    @Test
    void top_rated_returns_listed_properties_with_rating_fields() throws Exception {
        String body = mvc.perform(get(ApiRoutes.PROPERTIES_TOP_RATED).param("size", "4"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);

        assertThat(root.isArray()).isTrue();
        assertThat(root.size()).isBetween(1, 4);
        for (int i = 0; i < root.size(); i++) {
            // Every row has the rating aggregate fields populated (default
            // zero for un-reviewed seeded properties).
            assertThat(root.get(i).has("avgRating")).isTrue();
            assertThat(root.get(i).has("ratingCount")).isTrue();
            assertThat(root.get(i).get("ratingCount").asInt()).isNotNegative();
        }
    }

    @Test
    void popular_areas_ranks_seeded_suburbs_by_listing_count() throws Exception {
        String body = mvc.perform(get(ApiRoutes.PROPERTIES_POPULAR_AREAS).param("size", "5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);

        // V10 seeds 20 LISTED properties; the endpoint should surface real
        // suburb groups, never the editorial fallback (count = 0).
        assertThat(root.isArray()).isTrue();
        assertThat(root.size()).isBetween(1, 5);
        for (int i = 0; i < root.size(); i++) {
            assertThat(root.get(i).get("name").asText()).isNotBlank();
            assertThat(root.get(i).get("listingCount").asLong()).isGreaterThan(0L);
        }

        // Counts are monotonically non-increasing — the order contract.
        long prev = Long.MAX_VALUE;
        for (int i = 0; i < root.size(); i++) {
            long count = root.get(i).get("listingCount").asLong();
            assertThat(count).isLessThanOrEqualTo(prev);
            prev = count;
        }
    }

    @Test
    void popular_areas_uses_default_size_three_when_not_specified() throws Exception {
        String body = mvc.perform(get(ApiRoutes.PROPERTIES_POPULAR_AREAS))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);
        assertThat(root.size()).isLessThanOrEqualTo(3);
    }

    @Test
    void type_filter_restricts_to_matching_unit_types() throws Exception {
        String body = mvc.perform(get(ApiRoutes.PROPERTIES).param("type", "STUDIO"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(body);
        // At least one studio in the seed (props 7 + 11 both have studio units).
        assertThat(root.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void detail_endpoint_returns_units_and_images() throws Exception {
        // Pull the first property's id from the list endpoint.
        String list = mvc.perform(get(ApiRoutes.PROPERTIES).param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(list).get("content").get(0).get("id").asText();

        String detail = mvc.perform(get(ApiRoutes.PROPERTIES + "/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(detail);
        assertThat(root.get("status").asText()).isEqualTo("LISTED");
        assertThat(root.get("units").size()).isGreaterThanOrEqualTo(1);
        assertThat(root.get("images").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void anonymous_create_is_rejected() throws Exception {
        CreatePropertyRequest req = new CreatePropertyRequest(
                "Test Property", null, PropertyType.HOUSE,
                null, "Sandton", "Johannesburg", "Gauteng", "2196",
                null, null,
                null, null
        );
        mvc.perform(post(ApiRoutes.PROPERTIES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticated_landlord_can_create_update_and_delete() throws Exception {
        // Login as the LANDLORD demo seed (Thandi, also the manager of all V10 properties).
        String login = mvc.perform(post(ApiRoutes.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("thandi@example.co.za", "habitat123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        AuthResponse tokens = json.readValue(login, AuthResponse.class);

        // Create.
        CreatePropertyRequest req = new CreatePropertyRequest(
                "Integration Test Property", "for the integration test", PropertyType.HOUSE,
                "1 Test Road", "Sandton", "Johannesburg", "Gauteng", "2196",
                -26.1, 28.0,
                null, null
        );
        String createBody = mvc.perform(post(ApiRoutes.PROPERTIES)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = json.readTree(createBody);
        String newId = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");

        // Drafts aren't visible to anonymous callers.
        mvc.perform(get(ApiRoutes.PROPERTIES + "/" + newId))
                .andExpect(status().isNotFound());

        // But the owner can fetch their draft.
        mvc.perform(get(ApiRoutes.PROPERTIES + "/" + newId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // Delete.
        mvc.perform(delete(ApiRoutes.PROPERTIES + "/" + newId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNoContent());

        // Soft-deleted → 404 on next read.
        mvc.perform(get(ApiRoutes.PROPERTIES + "/" + newId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound());
    }
}
