package com.habitat.api.repository;

import com.habitat.api.entity.Landlord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * One method per query the service layer actually needs. Lookups by
 * {@code id_number} are the dedup key for offline-landlord capture;
 * lookups by {@code user.id} drive {@code /properties/owned-by-me} via
 * a join in {@link PropertyRepository}.
 */
public interface LandlordRepository extends JpaRepository<Landlord, UUID> {

    /** Dedup key: returns the canonical Landlord row for an SA ID. */
    Optional<Landlord> findByIdNumber(String idNumber);

    /** The ONLINE row backing a Habitat user, if any. */
    Optional<Landlord> findByUser_Id(UUID userId);
}
