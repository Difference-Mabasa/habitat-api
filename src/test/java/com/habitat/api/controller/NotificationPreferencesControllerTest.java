package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.notification.NotificationPreferenceMatrix;
import com.habitat.api.dto.notification.NotificationPreferenceUpdate;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.exception.ValidationException;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.service.NotificationPreferencesService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationPreferencesController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationPreferencesControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean NotificationPreferencesService preferences;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    @Test
    void matrix_returns_categories_channels_cells() throws Exception {
        NotificationPreferenceMatrix.Cell cell = new NotificationPreferenceMatrix.Cell(
                NotificationCategory.SYSTEM, NotificationChannel.IN_APP, true, true);
        when(preferences.getMyMatrix()).thenReturn(new NotificationPreferenceMatrix(
                List.of(NotificationCategory.values()),
                List.of(NotificationChannel.values()),
                List.of(cell)));

        mvc.perform(get(ApiRoutes.PREFERENCES_NOTIFICATIONS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0]").value("SYSTEM"))
                .andExpect(jsonPath("$.channels[0]").value("IN_APP"))
                .andExpect(jsonPath("$.cells[0].category").value("SYSTEM"))
                .andExpect(jsonPath("$.cells[0].locked").value(true))
                .andExpect(jsonPath("$.cells[0].enabled").value(true));
    }

    @Test
    void update_returns_204() throws Exception {
        mvc.perform(patch(ApiRoutes.PREFERENCES_NOTIFICATIONS)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new NotificationPreferenceUpdate(
                                NotificationCategory.MARKETING,
                                NotificationChannel.EMAIL,
                                false))))
                .andExpect(status().isNoContent());
    }

    @Test
    void update_returns_422_when_attempting_to_mute_SYSTEM_IN_APP() throws Exception {
        doThrow(new ValidationException("Account & security in-app alerts can't be muted."))
                .when(preferences).update(any());

        mvc.perform(patch(ApiRoutes.PREFERENCES_NOTIFICATIONS)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new NotificationPreferenceUpdate(
                                NotificationCategory.SYSTEM,
                                NotificationChannel.IN_APP,
                                false))))
                .andExpect(status().isUnprocessableEntity());
    }
}
