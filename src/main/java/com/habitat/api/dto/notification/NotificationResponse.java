package com.habitat.api.dto.notification;

import com.habitat.api.entity.Notification;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationType;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public shape of a notification on the wire. Mirrors the entity's
 * presentation fields and intentionally omits {@code refId} since the
 * action URL already carries the full route the UI needs.
 *
 * {@code category} is exposed so the drawer can show a tag / icon hint
 * per row (account-security alerts visually distinct from marketing).
 */
@Builder
public record NotificationResponse(
        UUID id,
        NotificationCategory category,
        NotificationType type,
        String title,
        String body,
        String actionUrl,
        String actionLabel,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .category(n.getCategory())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .actionUrl(n.getActionUrl())
                .actionLabel(n.getActionLabel())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
