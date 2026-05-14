package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.FurnishingType;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A rentable space inside a {@link Property}. PropertyCards on /browse
 * surface one Unit per Property (cheapest available); /property/{id}
 * detail lists every unit.
 *
 * All money values are {@link BigDecimal} ZAR.
 */
@Entity
@Table(name = "units")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class Unit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "unit_number", length = 40)
    private String unitNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 40)
    private UnitType unitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private UnitStatus status = UnitStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "furnishing", length = 20)
    private FurnishingType furnishing;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_frequency", nullable = false, length = 16)
    @Builder.Default
    private PaymentFrequency paymentFrequency = PaymentFrequency.MONTHLY;

    @Column(name = "deposit", precision = 12, scale = 2)
    private BigDecimal deposit;

    @Column(name = "bedrooms", nullable = false)
    private Integer bedrooms;

    @Column(name = "bathrooms", nullable = false)
    private Integer bathrooms;

    @Column(name = "sqm")
    private Integer sqm;

    @Column(name = "max_occupants")
    private Integer maxOccupants;

    @Column(name = "water_included")
    @Builder.Default
    private Boolean waterIncluded = false;

    @Column(name = "electricity_included")
    @Builder.Default
    private Boolean electricityIncluded = false;

    @Column(name = "pets_allowed")
    @Builder.Default
    private Boolean petsAllowed = false;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    // PERSIST + MERGE only — see Property.units rationale (TECH_DEBT.md ARCH-01).
    @OneToMany(mappedBy = "unit",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, createdAt ASC")
    @Builder.Default
    private List<UnitImage> images = new ArrayList<>();
}
