package com.habitat.api.repository;

import com.habitat.api.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

    /** All amenities sorted by the seed's intended display order. */
    List<Amenity> findAllByOrderBySortOrderAscNameAsc();

    List<Amenity> findAllByIdIn(List<UUID> ids);
}
