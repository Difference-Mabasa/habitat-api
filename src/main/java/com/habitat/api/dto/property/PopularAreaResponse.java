package com.habitat.api.dto.property;

/**
 * Public payload for {@code GET /properties/popular-areas}. One row per
 * suburb that currently has LISTED properties, ordered by listing count
 * (ties broken by name).
 *
 * <p>{@code listingCount} is {@code 0} for editorial-fallback rows
 * returned when no real listings exist yet — clients shouldn't treat
 * zero as an error; it just means "this suburb is featured, not ranked".
 */
public record PopularAreaResponse(
        String name,
        long listingCount
) {}
