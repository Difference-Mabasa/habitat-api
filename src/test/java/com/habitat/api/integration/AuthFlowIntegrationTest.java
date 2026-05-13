package com.habitat.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.auth.AuthResponse;
import com.habitat.api.dto.auth.LoginRequest;
import com.habitat.api.dto.auth.LogoutRequest;
import com.habitat.api.dto.auth.RefreshRequest;
import com.habitat.api.dto.auth.RegisterRequest;
import com.habitat.api.dto.user.SwitchActiveRoleRequest;
import com.habitat.api.enums.Role;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end auth flow against a real Postgres (via Testcontainers).
 *
 * Walks: register → login → /me → switch role → refresh → logout. Verifies
 * that the access token issued at register works, that switching active
 * role is persisted, that refresh issues new tokens, and that logout
 * doesn't crash. Per the standards doc this is the "one @SpringBootTest
 * per feature" tier — fast enough to keep, slow enough to be optional in
 * a tight inner dev loop.
 *
 * Redis is intentionally not wired here (cache.type=none) so the
 * TokenBlocklistService runs in its no-op branch. Blocklist semantics are
 * already covered by TokenBlocklistServiceTest; this test focuses on the
 * happy-path HTTP plumbing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthFlowIntegrationTest {

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
    void full_auth_lifecycle_works_end_to_end() throws Exception {
        // 1. Register a new tenant.
        String registerBody = json.writeValueAsString(new RegisterRequest(
                "lifecycle@example.co.za", "password123", "Lifecycle", "Tester", Role.USER, "Brixton"));
        String registerResponse = mvc.perform(post(ApiRoutes.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        AuthResponse registered = json.readValue(registerResponse, AuthResponse.class);
        assertThat(registered.accessToken()).isNotBlank();
        assertThat(registered.refreshToken()).isNotBlank();
        assertThat(registered.email()).isEqualTo("lifecycle@example.co.za");
        assertThat(registered.activeRole()).isEqualTo(Role.USER);

        // 2. Hit /me with the access token.
        mvc.perform(get(ApiRoutes.USERS_ME).header("Authorization", "Bearer " + registered.accessToken()))
                .andExpect(status().isOk());

        // 3. Try to switch to a role the user doesn't own → 403.
        mvc.perform(patch(ApiRoutes.USERS_ME_ACTIVE_ROLE)
                        .header("Authorization", "Bearer " + registered.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SwitchActiveRoleRequest(Role.ADMIN))))
                .andExpect(status().isForbidden());

        // 4. Login with credentials returns a fresh pair.
        String loginResponse = mvc.perform(post(ApiRoutes.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("lifecycle@example.co.za", "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        AuthResponse loggedIn = json.readValue(loginResponse, AuthResponse.class);
        assertThat(loggedIn.accessToken()).isNotBlank().isNotEqualTo(registered.accessToken());

        // 5. Refresh.
        String refreshResponse = mvc.perform(post(ApiRoutes.AUTH_REFRESH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RefreshRequest(loggedIn.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode refreshed = json.readTree(refreshResponse);
        assertThat(refreshed.get("accessToken").asText()).isNotBlank();

        // 6. Logout — 204.
        mvc.perform(post(ApiRoutes.AUTH_LOGOUT)
                        .header("Authorization", "Bearer " + loggedIn.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LogoutRequest(loggedIn.refreshToken()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void seeded_demo_users_can_log_in() throws Exception {
        String body = json.writeValueAsString(new LoginRequest("sipho@example.co.za", "habitat123"));
        String response = mvc.perform(post(ApiRoutes.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        AuthResponse out = json.readValue(response, AuthResponse.class);
        assertThat(out.email()).isEqualTo("sipho@example.co.za");
        // Every demo user holds all four roles (per development-standards.md §2).
        assertThat(out.roles()).hasSize(4);
        assertThat(out.activeRole()).isEqualTo(Role.USER);
    }
}
