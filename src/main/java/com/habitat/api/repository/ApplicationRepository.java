package com.habitat.api.repository;

import com.habitat.api.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /**
     * Tenant can only have one active (non-soft-deleted) application per
     * unit. {@link com.habitat.api.entity.Application} carries a
     * {@code @SQLRestriction} that already filters deleted rows, so this
     * boolean is honest without an extra deletedAt check.
     */
    boolean existsByUnit_IdAndTenant_Id(UUID unitId, UUID tenantId);

    /** All applications submitted by a tenant. Newest first. */
    List<Application> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

    /**
     * All applications received against units of properties the user
     * manages — the landlord inbox.
     */
    List<Application> findByUnit_Property_Manager_IdOrderByCreatedAtDesc(UUID managerId);
}
