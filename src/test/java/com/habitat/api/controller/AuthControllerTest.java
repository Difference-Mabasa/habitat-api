package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.auth.AuthResponse;
import com.habitat.api.dto.auth.LoginRequest;
import com.habitat.api.dto.auth.LogoutRequest;
import com.habitat.api.dto.auth.RefreshRequest;
import com.habitat.api.dto.auth.RegisterRequest;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.UnauthorizedException;
import com.habitat.api.security.HabitatPrincipal;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.service.AuthService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // addFilters=false bypasses Spring Security's filter chain, so
    // SecurityMockMvcRequestPostProcessors.authentication(...) has nowhere to
    // populate SecurityContextHolder for the request. We seed the context
    // manually for the /logout tests and clear it afterwards.
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Authentication authFor(HabitatPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(
                principal, null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + principal.activeRole())));
    }

    // ── /register ──────────────────────────────────────────────────────────

    @Test
    void register_returns_201_with_tokens() throws Exception {
        when(authService.register(any())).thenReturn(sampleAuthResponse());

        mvc.perform(post(ApiRoutes.AUTH_REGISTER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterRequest(
                                "a@example.co.za", "password123", "Sipho", "Dlamini", Role.TENANT, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.activeRole").value("TENANT"));
    }

    @Test
    void register_returns_422_for_invalid_payload() throws Exception {
        mvc.perform(post(ApiRoutes.AUTH_REGISTER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "not-an-email", "password": "short", "firstName": "", "surname": "", "role": "TENANT" }"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void register_returns_409_on_duplicate_email() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("dup"));

        mvc.perform(post(ApiRoutes.AUTH_REGISTER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterRequest(
                                "a@example.co.za", "password123", "Sipho", "Dlamini", Role.TENANT, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    // ── /login ─────────────────────────────────────────────────────────────

    @Test
    void login_with_valid_credentials_returns_tokens() throws Exception {
        when(authService.login(any())).thenReturn(sampleAuthResponse());

        mvc.perform(post(ApiRoutes.AUTH_LOGIN)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("a@example.co.za", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_with_wrong_credentials_returns_401() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("bad"));

        mvc.perform(post(ApiRoutes.AUTH_LOGIN)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("a@example.co.za", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    // ── /refresh ───────────────────────────────────────────────────────────

    @Test
    void refresh_returns_new_token_pair() throws Exception {
        when(authService.refresh(eq("rt"))).thenReturn(sampleAuthResponse());

        mvc.perform(post(ApiRoutes.AUTH_REFRESH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RefreshRequest("rt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void refresh_with_revoked_token_returns_401() throws Exception {
        when(authService.refresh(any())).thenThrow(new UnauthorizedException("revoked"));

        mvc.perform(post(ApiRoutes.AUTH_REFRESH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RefreshRequest("rt"))))
                .andExpect(status().isUnauthorized());
    }

    // ── /logout ────────────────────────────────────────────────────────────

    @Test
    void logout_with_principal_returns_204() throws Exception {
        HabitatPrincipal principal = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.TENANT), Role.TENANT,
                "jti", Instant.now().plusSeconds(900));
        SecurityContextHolder.getContext().setAuthentication(authFor(principal));

        mvc.perform(post(ApiRoutes.AUTH_LOGOUT)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LogoutRequest("rt"))))
                .andExpect(status().isNoContent());

        verify(authService).logout(eq(principal), eq("rt"));
    }

    @Test
    void logout_with_no_body_revokes_only_access_token() throws Exception {
        HabitatPrincipal principal = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.TENANT), Role.TENANT,
                "jti", Instant.now().plusSeconds(900));
        SecurityContextHolder.getContext().setAuthentication(authFor(principal));

        mvc.perform(post(ApiRoutes.AUTH_LOGOUT).with(csrf()))
                .andExpect(status().isNoContent());

        verify(authService).logout(eq(principal), eq(null));
    }

    private static AuthResponse sampleAuthResponse() {
        return AuthResponse.builder()
                .accessToken("access-token")
                .accessTokenExpiresAt(Instant.now().plusSeconds(900))
                .refreshToken("refresh-token")
                .refreshTokenExpiresAt(Instant.now().plusSeconds(86400))
                .userId(USER_ID)
                .email("a@example.co.za")
                .firstName("Sipho")
                .surname("Dlamini")
                .roles(Set.of(Role.TENANT))
                .activeRole(Role.TENANT)
                .build();
    }
}
