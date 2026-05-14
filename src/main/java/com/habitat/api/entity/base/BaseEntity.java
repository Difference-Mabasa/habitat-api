package com.habitat.api.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base class for all JPA entities.
 *
 * Ships with the lessons from backroom-api built in:
 *  - Spring Data JPA auditing (not Hibernate-proprietary @CreationTimestamp).
 *  - createdBy / updatedBy populated by AuditorAware&lt;UUID&gt; — see JpaAuditingConfig.
 *  - Soft-delete column from commit 1. Concrete entities add
 *    {@code @SQLRestriction("deleted_at IS NULL")} as needed.
 *  - equals/hashCode keyed on id (null-safe) so detached vs proxy comparisons
 *    inside Sets/Maps don't surprise us.
 *
 * @see com.habitat.api.config.JpaAuditingConfig
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(of = "id")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Soft-delete timestamp. When non-null, the row is considered deleted.
     * Mutable so service-layer code can flip it via setter.
     */
    @Setter
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Optimistic-lock version. Hibernate auto-increments on every write
     * and throws {@link jakarta.persistence.OptimisticLockException} on
     * stale updates — addresses {@code BUG-01} in
     * {@code habitat-api/TECH_DEBT.md}.
     *
     * <p>Migration V24 backfills 0 on existing rows.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
