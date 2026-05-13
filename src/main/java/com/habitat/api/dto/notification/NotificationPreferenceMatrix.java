package com.habitat.api.dto.notification;

import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;

import java.util.List;

/**
 * Response shape for GET /preferences/notifications. A flat matrix the
 * UI renders as a table: one entry per (category, channel) cell, with
 * {@code locked = true} on cells the user can't change.
 *
 * Categories + channels are emitted in their enum declaration order so
 * the UI can rely on a stable layout without sorting.
 */
public record NotificationPreferenceMatrix(
        List<NotificationCategory> categories,
        List<NotificationChannel> channels,
        List<Cell> cells
) {
    public record Cell(
            NotificationCategory category,
            NotificationChannel channel,
            boolean enabled,
            boolean locked
    ) {}
}
