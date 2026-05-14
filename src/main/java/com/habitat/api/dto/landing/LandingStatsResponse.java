package com.habitat.api.dto.landing;

/**
 * Public payload for {@code GET /landing/stats}. Three honest counts that
 * power the landlord-hero trust strip on the marketing landing page.
 *
 * <ul>
 *   <li>{@code activeListings} — properties with status {@code LISTED}.</li>
 *   <li>{@code registeredTenants} — users with role {@code USER}.
 *       Notes that this includes accounts that also manage properties (the
 *       {@code USER} role covers both tenants and landlords per the Role
 *       enum's docs); the landing copy labels this stat
 *       "Registered tenants" because pre-launch the data isn't fine-grained
 *       enough to distinguish lease-holders from landlords-only.</li>
 *   <li>{@code suburbsCovered} — distinct non-blank suburb values across
 *       {@code LISTED} properties.</li>
 * </ul>
 *
 * All counts are {@code long} so JSON serialisation never silently
 * truncates as the catalogue grows.
 */
public record LandingStatsResponse(
        long activeListings,
        long registeredTenants,
        long suburbsCovered
) {}
