package com.habitat.api.repository;

import com.habitat.api.entity.Viewing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ViewingRepository extends JpaRepository<Viewing, UUID> {

    /** Tenant's own viewings, newest scheduled-time first. */
    List<Viewing> findByTenant_IdOrderByScheduledAtDesc(UUID tenantId);

    /**
     * Viewings where the caller is the manager of the unit's
     * property — backs GET /viewings/managed. Joins
     * Viewing → Unit → Property → manager.
     */
    List<Viewing> findByUnit_Property_Manager_IdOrderByScheduledAtAsc(UUID managerId);
}
