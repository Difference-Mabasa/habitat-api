package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.notification.NotificationPreferenceMatrix;
import com.habitat.api.dto.notification.NotificationPreferenceUpdate;
import com.habitat.api.service.NotificationPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.PREFERENCES_NOTIFICATIONS)
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferences;

    @GetMapping
    public NotificationPreferenceMatrix matrix() {
        return preferences.getMyMatrix();
    }

    @PatchMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@Valid @RequestBody NotificationPreferenceUpdate req) {
        preferences.update(req);
    }
}
