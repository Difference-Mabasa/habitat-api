package com.habitat.api.enums;

/**
 * The actual rentable space inside a {@link com.habitat.api.entity.Property}.
 * Drives the Type filter on /browse and PropertyCard headlines.
 *
 * Premium-aligned — backroom-ui's BACKROOM / ROOM / GRANNY_FLAT / GARDEN_FLAT
 * values are intentionally absent; if we ever target that audience again we
 * can {@code ALTER TYPE ... ADD VALUE} on the Postgres enum without
 * recreating the type.
 *
 * Stored as STRING.
 */
public enum UnitType {
    APARTMENT,
    HOUSE,
    TOWNHOUSE,
    COTTAGE,
    STUDIO,
    FLATLET
}
