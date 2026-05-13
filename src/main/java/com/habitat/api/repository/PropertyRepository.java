package com.habitat.api.repository;

import com.habitat.api.entity.Property;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.UnitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    /**
     * Public search across LISTED properties. Filters are AND-combined; null
     * = "ignore this filter". Location matches case-insensitively across
     * suburb/city/province so a user typing "Sandton" finds it whether it
     * resolved as suburb or city via Google Places.
     *
     * Unit-level filters (type / maxPrice / minBeds) are combined into a
     * single EXISTS so a property only appears if it has at least one
     * AVAILABLE unit satisfying ALL of them — the user expects "Apartment
     * + maxPrice 15000 + 2-bed" to mean one apartment with those traits,
     * not three different units each matching one filter.
     */
    @Query("""
            SELECT p FROM Property p
            WHERE p.status = :listedStatus
              AND (:location = '' OR LOWER(p.suburb) LIKE LOWER(CONCAT('%', :location, '%'))
                                  OR LOWER(p.city) LIKE LOWER(CONCAT('%', :location, '%'))
                                  OR LOWER(p.province) LIKE LOWER(CONCAT('%', :location, '%')))
              AND EXISTS (
                    SELECT 1 FROM Unit u
                    WHERE u.property = p
                      AND u.status = com.habitat.api.enums.UnitStatus.AVAILABLE
                      AND (:unitTypes IS NULL OR u.unitType IN :unitTypes)
                      AND (:maxPrice IS NULL OR u.price <= :maxPrice)
                      AND (:minBeds IS NULL OR u.bedrooms >= :minBeds)
              )
            ORDER BY p.createdAt DESC
            """)
    Page<Property> search(
            @Param("listedStatus") PropertyStatus listedStatus,
            /* Empty string = "no location filter". Passing null instead trips
               Postgres' bind-parameter type inference (lower(bytea) error). */
            @Param("location") String location,
            @Param("unitTypes") List<UnitType> unitTypes,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minBeds") Integer minBeds,
            Pageable pageable
    );

    /**
     * Authenticated landlord/agent's properties — every status, not just
     * LISTED. Manager owns the relationship today; landlord may differ
     * (agent-managed listings).
     */
    Page<Property> findByManagerIdOrderByCreatedAtDesc(UUID managerId, Pageable pageable);
}
