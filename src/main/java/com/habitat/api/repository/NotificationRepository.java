package com.habitat.api.repository;

import com.habitat.api.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    /**
     * Bulk mark-as-read. Returns the row count for the caller to assert on
     * if it cares. Uses a JPQL update — bypasses the entity listener, so we
     * stamp {@code updatedAt} + {@code readAt} explicitly.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
               SET n.read = true,
                   n.readAt = :now,
                   n.updatedAt = :now
             WHERE n.recipient.id = :recipientId
               AND n.read = false
            """)
    int markAllAsRead(@Param("recipientId") UUID recipientId, @Param("now") OffsetDateTime now);
}
