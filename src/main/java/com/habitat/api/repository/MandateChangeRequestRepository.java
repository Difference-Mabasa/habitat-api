package com.habitat.api.repository;

import com.habitat.api.entity.MandateChangeRequest;
import com.habitat.api.enums.ChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MandateChangeRequestRepository extends JpaRepository<MandateChangeRequest, UUID> {

    /** Most recent OPEN request for this mandate (typically there's
     *  only one open at a time, but ordering by requestedAt guards
     *  against the "agent didn't address; landlord requested again"
     *  edge case before the defensive sweep runs). */
    Optional<MandateChangeRequest> findFirstByMandate_IdAndStatusOrderByRequestedAtDesc(
            UUID mandateId, ChangeRequestStatus status);

    /** All change requests for this mandate, oldest first — drives
     *  the history thread when scoped to a single round. */
    List<MandateChangeRequest> findByMandate_IdOrderByRequestedAtAsc(UUID mandateId);

    /** Every change request across every mandate row a property has
     *  ever had. Drives the cross-round history thread now that V41
     *  enforces one non-terminal mandate per property — earlier rounds
     *  end up on REJECTED rows that still own their change_requests. */
    List<MandateChangeRequest> findByMandate_Property_IdOrderByRequestedAtAsc(UUID propertyId);

    /** All OPEN requests for a mandate — used by the defensive sweep
     *  on approve / reject / resubmit / withdraw to mark stragglers
     *  as ADDRESSED or WITHDRAWN. */
    List<MandateChangeRequest> findByMandate_IdAndStatus(UUID mandateId, ChangeRequestStatus status);
}
