package com.habitat.api.dto.landing;

/**
 * Public payload for {@code GET /landing/cities}. One row per city that
 * currently has LISTED properties, ordered by listing count (ties broken
 * by name).
 *
 * <p>{@code listingCount} is {@code 0} for editorial-fallback rows
 * returned when no real listings exist yet — clients shouldn't treat
 * zero as an error; the city is featured, not ranked.
 *
 * <p>Distinct from {@link com.habitat.api.dto.property.PopularAreaResponse}
 * which is at suburb granularity. The two endpoints power different
 * surfaces on the landing page (suburb chips above the search bar, city
 * marquee in the "Trusted across" strip) — we keep them as separate
 * records so they can diverge in shape later without breaking each other.
 */
public record PopularCityResponse(
        String name,
        long listingCount
) {}
