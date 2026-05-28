package com.habitat.api.repository;

import com.habitat.api.entity.Mandate;
import com.habitat.api.enums.MandateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MandateRepository extends JpaRepository<Mandate, UUID> {

    /** The most recent mandate row for a property (any status).
     *  Note: after V41 there's at most one non-terminal row per
     *  property; older REJECTED/EXPIRED rows can coexist as history.
     *  This method still returns the latest by createdAt, which is
     *  typically the non-terminal one when present. */
    Optional<Mandate> findFirstByProperty_IdOrderByCreatedAtDesc(UUID propertyId);

    /** Every mandate row for a property, oldest-first. Drives the
     *  cross-round history thread on the detail screens. */
    List<Mandate> findByProperty_IdOrderByCreatedAtAsc(UUID propertyId);

    /** True when the property has a non-terminal (in-flight) mandate.
     *  Drives the {@code issue} conflict check — the V41 partial
     *  unique index would otherwise be the only enforcement, but
     *  catching it in service gives a clean 409 instead of a
     *  500-shaped DB constraint exception. */
    boolean existsByProperty_IdAndStatusNotIn(UUID propertyId, Iterable<MandateStatus> terminalStatuses);


    /**
     * Mandates currently awaiting a given user's landlord-side
     * approval — i.e. they're the resolved owner on the property's
     * Landlord row, and the mandate's still at
     * PENDING_LANDLORD_APPROVAL. Drives /mandate-approvals.
     */
    Page<Mandate> findByStatusAndProperty_Landlord_User_IdOrderByCreatedAtDesc(
            MandateStatus status, UUID userId, Pageable pageable);

    /** Slice 4: agent's inbox — all mandates they issued, optionally
     *  filtered by status. Sorted by updatedAt DESC so anything the
     *  landlord just touched bubbles to the top. */
    Page<Mandate> findByAgent_IdOrderByUpdatedAtDesc(UUID agentId, Pageable pageable);

    Page<Mandate> findByAgent_IdAndStatusOrderByUpdatedAtDesc(
            UUID agentId, MandateStatus status, Pageable pageable);
}
