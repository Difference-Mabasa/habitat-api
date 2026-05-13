package com.habitat.api.event;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.dto.notification.PushResult;
import com.habitat.api.entity.User;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WelcomeNotificationListenerTest {

    @Mock NotificationService notifications;

    @InjectMocks WelcomeNotificationListener listener;

    @Test
    void onUserRegistered_pushes_welcome_with_first_name_in_body() {
        User u = User.builder()
                .email("lerato@example.co.za")
                .firstName("Lerato")
                .surname("Khumalo")
                .build();
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PushResult(Map.of()));

        listener.onUserRegistered(new UserRegisteredEvent(UUID.randomUUID(), u));

        ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
        verify(notifications).push(
                org.mockito.ArgumentMatchers.eq(u),
                org.mockito.ArgumentMatchers.eq(NotificationType.WELCOME),
                org.mockito.ArgumentMatchers.eq(NotificationMessages.WELCOME_TITLE),
                bodyCap.capture(),
                org.mockito.ArgumentMatchers.eq(ApiRoutes.UI_PROFILE),
                org.mockito.ArgumentMatchers.eq(NotificationMessages.WELCOME_ACTION_LABEL),
                org.mockito.ArgumentMatchers.isNull());

        assertThat(bodyCap.getValue()).contains("Hi Lerato!");
        assertThat(bodyCap.getValue()).doesNotContain("{firstName}");
    }

    @Test
    void onUserRegistered_swallows_push_failures() {
        User u = User.builder()
                .email("x@example.co.za")
                .firstName("X")
                .surname("Y")
                .build();
        when(notifications.push(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("downstream blew up"));

        listener.onUserRegistered(new UserRegisteredEvent(UUID.randomUUID(), u));
    }
}
