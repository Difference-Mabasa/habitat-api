package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.notification.NotificationResponse;
import com.habitat.api.dto.notification.UnreadCountResponse;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.NotificationService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired @SuppressWarnings("unused") ObjectMapper json;
    @MockBean NotificationService notificationService;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID NOTIF_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void list_returns_a_page_of_notifications() throws Exception {
        PageResponse<NotificationResponse> page = PageResponse.<NotificationResponse>builder()
                .content(List.of(sample()))
                .page(0).size(20).totalElements(1L).totalPages(1)
                .build();
        when(notificationService.getMyNotifications(0, 20)).thenReturn(page);

        mvc.perform(get(ApiRoutes.NOTIFICATIONS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(NOTIF_ID.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Welcome to Habitat!"))
                .andExpect(jsonPath("$.content[0].actionUrl").value("/profile/onboarding"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void unreadCount_returns_count_payload() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(new UnreadCountResponse(7L));

        mvc.perform(get(ApiRoutes.NOTIFICATIONS_UNREAD_COUNT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    @Test
    void markAsRead_returns_updated_notification() throws Exception {
        NotificationResponse out = sampleRead();
        when(notificationService.markAsRead(NOTIF_ID)).thenReturn(out);

        mvc.perform(patch(ApiRoutes.NOTIFICATIONS + "/" + NOTIF_ID + "/read").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAllAsRead_returns_204() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(3);

        mvc.perform(patch(ApiRoutes.NOTIFICATIONS_READ_ALL).with(csrf()))
                .andExpect(status().isNoContent());
        verify(notificationService).markAllAsRead();
    }

    @Test
    void delete_returns_204() throws Exception {
        mvc.perform(delete(ApiRoutes.NOTIFICATIONS + "/" + NOTIF_ID).with(csrf()))
                .andExpect(status().isNoContent());
        verify(notificationService).delete(eq(NOTIF_ID));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static NotificationResponse sample() {
        return NotificationResponse.builder()
                .id(NOTIF_ID)
                .category(com.habitat.api.enums.NotificationCategory.ONBOARDING)
                .type(NotificationType.WELCOME)
                .title("Welcome to Habitat!")
                .body("Body text")
                .actionUrl("/profile/onboarding")
                .actionLabel("Complete Profile")
                .read(false)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private static NotificationResponse sampleRead() {
        return NotificationResponse.builder()
                .id(NOTIF_ID)
                .category(com.habitat.api.enums.NotificationCategory.ONBOARDING)
                .type(NotificationType.WELCOME)
                .title("Welcome to Habitat!")
                .body("Body text")
                .actionUrl("/profile/onboarding")
                .actionLabel("Complete Profile")
                .read(true)
                .readAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
