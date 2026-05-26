package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.ListingMode;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.PropertyType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A real-estate listing — the building, complex, or stand. Rentable space
 * inside lives in {@link Unit}.
 *
 * Roles (carried from backroom semantics):
 * <ul>
 *   <li>{@code landlord} — registered legal owner. Always a {@link User}.</li>
 *   <li>{@code manager} — platform contact on the listing. May be the
 *       landlord or their agent. Denormalised onto units for query speed
 *       (kept in sync at the service layer when changed).</li>
 * </ul>
 */
@Entity
@Table(name = "properties")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class Property extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 40)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.DRAFT;

    // ── Phase 4 (listing wizard) — mandate metadata ──────────────────
    // V27 captures the wizard's "Mandate" step inline on the property.
    // LANDLORD_DIRECT is the default; AGENT_MANAGED + the fee percent
    // are set when an agent placed the mandate. The full agent-mandate
    // state machine lands in Phase 12; these fields are the minimum
    // facts that survive the wizard until then.

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_mode", length = 20)
    @Builder.Default
    private ListingMode listingMode = ListingMode.LANDLORD_DIRECT;

    @Column(name = "mandate_fee_percent", precision = 5, scale = 2)
    private BigDecimal mandateFeePercent;

    // ── Structured address (mirrors the User.address shape) ───────────

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "suburb", length = 120)
    private String suburb;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "province", length = 80)
    private String province;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // ── Rating aggregation ───────────────────────────────────────────
    // Denormalised counters maintained out-of-band when a future review
    // service lands; surfaced via {@code GET /properties/top-rated} and
    // included on every PropertySummary so cards can show ★ + count.

    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private int ratingCount = 0;

    // ── Children ─────────────────────────────────────────────────────
    // Cascade PERSIST + MERGE so PATCH /properties/{id} can replace
    // collections atomically. Crucially NOT REMOVE / orphanRemoval:
    // after V22, units are RESTRICT-FK'd from leases + invoices, so a
    // cascading Hibernate DELETE would crash the moment a leased unit
    // is removed from the collection. Hard-delete is forbidden anyway
    // (development-standards.md §"Audit & deletion") — we soft-delete
    // via deletedAt. Addresses habitat-api/TECH_DEBT.md ARCH-01.

    @OneToMany(mappedBy = "property",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Unit> units = new ArrayList<>();

    @OneToMany(mappedBy = "property",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, createdAt ASC")
    @Builder.Default
    private List<PropertyImage> images = new ArrayList<>();

    /**
     * Property-level amenities (WiFi / Parking / Garden …). LinkedHashSet
     * preserves a stable iteration order across renders.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "property_amenities",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    @Builder.Default
    private Set<Amenity> amenities = new LinkedHashSet<>();
}
