package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.ViewingStatus;
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
 * A tenant's request to view a specific {@link Unit} at a scheduled
 * time. Status walks the {@link ViewingStatus} state machine — the
 * landlord/agent decides REQUESTED → APPROVED | REJECTED; either
 * side can move APPROVED → CANCELLED before the slot.
 *
 * <p>Doesn't carry a separate property FK — the unit's property is
 * the canonical link. Listener-side displays reach through
 * {@code viewing.unit.property} for title + address.
 */
@Entity
@Table(name = "viewings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class Viewing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_user_id", nullable = false)
    private User tenant;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ViewingStatus status = ViewingStatus.REQUESTED;

    /** Tenant's optional note to the landlord at request time. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Reviewer's note on approve / reject (e.g. "back-door access only"). */
    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    /** UUID of the user who decided (manager / agent). */
    @Column(name = "decided_by")
    private UUID decidedBy;

    /** UUID of the user who cancelled — either tenant or manager. */
    @Column(name = "cancelled_by")
    private UUID cancelledBy;
}
