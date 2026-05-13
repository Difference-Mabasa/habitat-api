package com.habitat.api.entity;

import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single per-user opt-in/out for a (category, channel) pair.
 *
 * Composite-keyed because there's exactly one row per user-cell of the
 * preferences matrix. A missing row is treated as opted-in by the
 * resolver — see {@link com.habitat.api.service.NotificationPreferencesService}.
 *
 * Doesn't extend BaseEntity because it has no UUID surrogate key, no
 * soft-delete, no createdBy/updatedBy auditing — just the bare audit
 * timestamps for diagnostics. A composite key suits the domain (one
 * row per cell) and keeps queries straightforward.
 */
@Entity
@Table(name = "notification_preferences")
@IdClass(NotificationPreference.PreferenceKey.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class NotificationPreference {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private NotificationCategory category;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Builder
    public static class PreferenceKey implements Serializable {
        private UUID userId;
        private NotificationCategory category;
        private NotificationChannel channel;
    }
}
