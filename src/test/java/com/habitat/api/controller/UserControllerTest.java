package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.user.SwitchActiveRoleRequest;
import com.habitat.api.dto.user.UserMeResponse;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.security.HabitatPrincipal;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.TokenBlocklistService;
import com.habitat.api.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean UserService userService;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void mockSecurity() {
        when(security.currentUserId()).thenReturn(Optional.of(USER_ID));
        when(security.requireUserId()).thenReturn(USER_ID);
    }

    // The 401-when-unauthenticated path is covered in AuthFlowIntegrationTest
    // where the real SecurityConfig chain runs.

    @Test
    void me_returns_the_authenticated_users_profile() throws Exception {
        when(userService.getMe(USER_ID)).thenReturn(sampleResponse(Role.USER));

        mvc.perform(get(ApiRoutes.USERS_ME).with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("a@example.co.za"))
                .andExpect(jsonPath("$.activeRole").value("USER"));
    }

    @Test
    void switchActiveRole_returns_updated_user() throws Exception {
        when(userService.switchActiveRole(eq(USER_ID), eq(Role.AGENT)))
                .thenReturn(sampleResponse(Role.AGENT));

        mvc.perform(patch(ApiRoutes.USERS_ME_ACTIVE_ROLE)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SwitchActiveRoleRequest(Role.AGENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("AGENT"));
    }

    @Test
    void switchActiveRole_returns_422_when_role_missing_from_body() throws Exception {
        mvc.perform(patch(ApiRoutes.USERS_ME_ACTIVE_ROLE)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void switchActiveRole_returns_403_when_role_not_owned() throws Exception {
        when(userService.switchActiveRole(any(), any())).thenThrow(new ForbiddenException("nope"));

        mvc.perform(patch(ApiRoutes.USERS_ME_ACTIVE_ROLE)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SwitchActiveRoleRequest(Role.ADMIN))))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.security.core.Authentication authToken() {
        HabitatPrincipal principal = new HabitatPrincipal(
                USER_ID, "a@example.co.za", Set.of(Role.USER), Role.USER,
                "jti", Instant.now().plusSeconds(900));
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT")));
    }

    private static UserMeResponse sampleResponse(Role activeRole) {
        return UserMeResponse.builder()
                .id(USER_ID)
                .email("a@example.co.za")
                .firstName("Sipho")
                .surname("Dlamini")
                .roles(Set.of(Role.USER, Role.AGENT))
                .activeRole(activeRole)
                .emailVerified(true)
                .area("Brixton")
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
