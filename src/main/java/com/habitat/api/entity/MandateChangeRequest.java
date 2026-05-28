package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.ChangeRequestStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One round of the landlord asking the agent to revise a mandate
 * before signing it. The whole row resolves when the agent either
 * resubmits (status flips to ADDRESSED) or withdraws (WITHDRAWN), or
 * when the landlord acts on the mandate directly while the request
 * is still OPEN (defensive WITHDRAWN sweep in service).
 *
 * <p>Multiple rows can exist per mandate (round 2, 3, n…); the
 * latest OPEN one drives the inbox + agent panel.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mandate_change_requests")
@SQLRestriction("deleted_at IS NULL")
public class MandateChangeRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandate_id", nullable = false)
    private Mandate mandate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /** Structured items — see {@link ChangeItem}. Stored as jsonb;
     *  Hibernate 6 serialises via Jackson under the hood. Initialised
     *  to an empty list so a null DB read still yields a non-null
     *  collection. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<ChangeItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ChangeRequestStatus status;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedByUser;
}
