package com.habitat.api.repository;

import com.habitat.api.entity.Mandate;
import com.habitat.api.enums.MandateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MandateRepository extends JpaRepository<Mandate, UUID> {

    /** The latest non-terminal mandate for a property, if any. */
    Optional<Mandate> findFirstByProperty_IdOrderByCreatedAtDesc(UUID propertyId);

    /**
     * Mandates currently awaiting a given user's landlord-side
     * approval — i.e. they're the resolved owner on the property's
     * Landlord row, and the mandate's still at
     * PENDING_LANDLORD_APPROVAL. Drives /mandate-approvals.
     */
    Page<Mandate> findByStatusAndProperty_Landlord_User_IdOrderByCreatedAtDesc(
            MandateStatus status, UUID userId, Pageable pageable);
}
