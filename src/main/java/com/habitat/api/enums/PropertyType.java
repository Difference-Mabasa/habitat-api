package com.habitat.api.enums;

/**
 * Top-level property type — the building or stand the listing represents.
 * Distinct from {@link UnitType}: a property of type APARTMENT_BLOCK
 * contains units of type APARTMENT, etc.
 *
 * Aligned with backroom-api's PropertyType. The split is strict: every
 * value here describes a BUILDING or PARCEL, never a rentable sub-space.
 *
 * <ul>
 *   <li>{@link #HOUSE} — single standalone house on its own stand.</li>
 *   <li>{@link #FLAT} — a single apartment listed standalone (parent of
 *       a single {@link UnitType#APARTMENT} unit). Use this rather than
 *       {@link #APARTMENT_BLOCK} when only one apartment is on offer.</li>
 *   <li>{@link #APARTMENT_BLOCK} — multi-unit residential building with
 *       two or more {@link UnitType#APARTMENT} units listed.</li>
 *   <li>{@link #TOWNHOUSE_COMPLEX} — gated estate of townhouses.</li>
 *   <li>{@link #COMPLEX} — generic mixed-use complex.</li>
 *   <li>{@link #PLOT} — bare land.</li>
 *   <li>{@link #FARM} — agricultural property.</li>
 * </ul>
 *
 * Stored as STRING (never ORDINAL). The properties.property_type column
 * is VARCHAR(40) with no DB-level CHECK constraint, so adding new values
 * here doesn't require a schema migration — only an entity build.
 */
public enum PropertyType {
    HOUSE,
    FLAT,
    APARTMENT_BLOCK,
    TOWNHOUSE_COMPLEX,
    COMPLEX,
    PLOT,
    FARM
}
