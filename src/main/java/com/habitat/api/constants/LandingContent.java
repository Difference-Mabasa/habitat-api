package com.habitat.api.constants;

import java.util.List;

/**
 * Editorial fallbacks for landing-page surfaces that need a sensible
 * default before live data is available.
 *
 * <p>The {@code GET /properties/popular-areas} endpoint groups LISTED
 * properties by suburb and returns the busiest ones; while the catalogue
 * is empty (pre-launch / fresh staging), the service falls back to this
 * list so the marketing surface never renders a bare "Popular: " label.
 *
 * <p>Keep the list short (≤ 4) and aspirational — these are the suburbs
 * the brand wants to anchor against. Once real listings build up, the
 * fallback drops out automatically.
 */
public final class LandingContent {

    /**
     * Hand-curated suburb names rendered when no LISTED properties exist
     * yet. Each value is used both as the chip label and as the value
     * passed to {@code /browse?location=…}, so it must match the
     * case-insensitive substring match in {@code PropertyRepository.search}.
     */
    public static final List<String> EDITORIAL_AREAS = List.of(
            "Sandton",
            "Umhlanga",
            "Camps Bay"
    );

    /**
     * Hand-curated SA city names rendered as the "Trusted across" marquee
     * fallback when the catalogue has no LISTED properties yet. Order is
     * the same hardcoded list the strip carried before the endpoint went
     * live; matches the case-insensitive substring matcher in
     * {@code PropertyRepository.search} so each value works as a
     * {@code /browse?location=…} target.
     */
    public static final List<String> EDITORIAL_CITIES = List.of(
            "Johannesburg",
            "Cape Town",
            "Durban",
            "Pretoria",
            "Gqeberha",
            "Polokwane",
            "Bloemfontein"
    );

    private LandingContent() {}
}
