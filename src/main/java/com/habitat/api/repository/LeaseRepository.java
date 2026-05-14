package com.habitat.api.repository;

import com.habitat.api.entity.Lease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

    /** Leases belonging to a tenant. Newest first. */
    List<Lease> findByApplication_Tenant_IdOrderByCreatedAtDesc(UUID tenantId);

    /** Leases on properties the user manages. */
    List<Lease> findByApplication_Unit_Property_Manager_IdOrderByCreatedAtDesc(UUID managerId);

    Optional<Lease> findByApplication_Id(UUID applicationId);
}
