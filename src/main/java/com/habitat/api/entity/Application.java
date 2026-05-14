package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.EmploymentStatus;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A tenant's application against a specific Unit. Backroom-aligned shape:
 * employment status + message + preferred move-in date, all optional.
 *
 * <p>Lifecycle starts at {@link ApplicationStatus#SUBMITTED}; richer
 * statuses (AWAITING_DOCUMENTS, APPROVED, REJECTED, etc.) land with the
 * landlord-review slice — for now SUBMITTED is the terminal state.
 *
 * <p>Soft-deleted via {@code BaseEntity.deletedAt} so a tenant can
 * withdraw + re-apply without an INSERT collision on
 * {@code uq_applications_tenant_unit}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "applications")
@SQLRestriction("deleted_at IS NULL")
public class Application extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", length = 40)
    private EmploymentStatus employmentStatus;

    /** Free-text landlord note attached on Approve / Reject / Hold. */
    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    /** UUID of the landlord/agent who made the call. Loose reference, no FK enforcement here. */
    @Column(name = "decided_by")
    private UUID decidedBy;
}
