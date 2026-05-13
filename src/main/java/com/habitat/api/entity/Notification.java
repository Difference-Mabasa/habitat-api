package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single in-app notification destined for one user.
 *
 * Extends {@link BaseEntity} so it inherits createdAt / updatedAt / soft-delete
 * (see development-standards §3). {@code readAt} is a separate field from
 * {@code updatedAt} so audit fields stay reserved for general row history.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 48)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /**
     * Optional ID of the resource this notification is about (e.g. the
     * application that's pending review). Generic so a single column can
     * point at any domain entity.
     */
    @Column(name = "ref_id")
    private UUID refId;

    /**
     * Where the notification's CTA navigates when tapped. Owned by the
     * backend so the UI doesn't have to map type → URL (avoids backroom's
     * brittle title-string CTA inference).
     */
    @Column(name = "action_url", length = 255)
    private String actionUrl;

    /** Label for the CTA button (e.g. "Complete profile"). */
    @Column(name = "action_label", length = 80)
    private String actionLabel;

    @Column(name = "read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /**
     * When the recipient acknowledged this notification. Set in the same
     * transaction as flipping {@link #read} to true. Lets a future scheduled
     * job prune long-acknowledged rows by age without losing the audit signal.
     */
    @Column(name = "read_at")
    private OffsetDateTime readAt;
}
