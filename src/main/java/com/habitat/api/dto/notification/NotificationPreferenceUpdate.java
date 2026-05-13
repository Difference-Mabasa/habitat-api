package com.habitat.api.dto.notification;

import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import jakarta.validation.constraints.NotNull;

/**
 * One-cell update to the preferences matrix. The PATCH endpoint applies
 * the change atomically; concurrent toggles in the UI shouldn't collide
 * because each cell is independent.
 */
public record NotificationPreferenceUpdate(
        @NotNull NotificationCategory category,
        @NotNull NotificationChannel channel,
        @NotNull Boolean enabled
) {}
