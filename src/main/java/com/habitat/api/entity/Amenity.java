package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * An amenity a tenant might filter on (WiFi, Parking, Garden …).
 * Seeded by V28 with the canonical backroom list (10 rows). Tagged
 * onto a {@link Property} via the {@code property_amenities} join.
 *
 * <p>{@code icon} stores a habitat-ui outline-icon name (matches the
 * {@code IconName} union in {@code components/Icon.tsx}), not a
 * Material Design ligature like backroom uses. Habitat's icon set is
 * smaller; the seed picks the closest match for each amenity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "amenities")
@SQLRestriction("deleted_at IS NULL")
public class Amenity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "icon", nullable = false, length = 40)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
