package com.habitat.api.dto.landing;

/**
 * Public payload for {@code GET /landing/stats}. Honest counts that power
 * the landlord-hero trust strip + floating proof cards on the landing.
 *
 * <ul>
 *   <li>{@code activeListings} — properties with status {@code LISTED}.</li>
 *   <li>{@code registeredTenants} — users whose active role is {@code USER}
 *       (covers both prospective tenants and landlord-only accounts per
 *       the Role enum's docs; surfaced under "Registered tenants" because
 *       pre-launch the data isn't fine-grained enough for stricter
 *       distinction).</li>
 *   <li>{@code suburbsCovered} — distinct non-blank suburb values across
 *       {@code LISTED} properties.</li>
 *   <li>{@code tenantsLast7Days} — USER-active-role accounts created
 *       within the rolling 7-day window ending now. Drives the "This week"
 *       momentum card; zero is a legitimate value during quiet weeks.</li>
 * </ul>
 *
 * All counts are {@code long} so JSON serialisation never silently
 * truncates as the catalogue grows.
 */
public record LandingStatsResponse(
        long activeListings,
        long registeredTenants,
        long suburbsCovered,
        long tenantsLast7Days
) {}
