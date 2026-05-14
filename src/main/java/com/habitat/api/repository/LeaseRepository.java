package com.habitat.api.repository;

import com.habitat.api.entity.Lease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

    /** Leases belonging to a tenant. Newest first. Reads off the direct {@code tenant_id} (V22). */
    List<Lease> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

    /** Leases on properties the user manages. Reads off the direct {@code landlord_id} (V22). */
    List<Lease> findByLandlord_IdOrderByCreatedAtDesc(UUID landlordId);

    /** Lookup by the optional trace pointer — for idempotent issuance from an application. */
    Optional<Lease> findByApplication_Id(UUID applicationId);
}
