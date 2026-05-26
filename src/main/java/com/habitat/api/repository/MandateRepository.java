package com.habitat.api.repository;

import com.habitat.api.entity.Mandate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MandateRepository extends JpaRepository<Mandate, UUID> {

    /** The latest non-terminal mandate for a property, if any. */
    Optional<Mandate> findFirstByProperty_IdOrderByCreatedAtDesc(UUID propertyId);
}
