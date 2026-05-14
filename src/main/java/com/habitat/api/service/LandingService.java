package com.habitat.api.service;

import com.habitat.api.constants.LandingContent;
import com.habitat.api.dto.landing.LandingStatsResponse;
import com.habitat.api.dto.landing.PopularCityResponse;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.Role;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cross-domain aggregator for marketing-page numbers. Lives in its own
 * service so neither {@link PropertyService} nor {@link UserService} grows
 * a "landing" concept that isn't really theirs.
 *
 * <p>All reads are anonymous / public; the controller is in
 * {@code PublicEndpoints} so no auth is required.
 */
@Service
@RequiredArgsConstructor
public class LandingService {

    /** Window the "This week" momentum card looks back across. */
    static final Duration MOMENTUM_WINDOW = Duration.ofDays(7);

    private final PropertyRepository properties;
    private final UserRepository users;

    /**
     * Defaults to system clock. Constructor-injectable for tests that need
     * a fixed instant to assert against the {@link #MOMENTUM_WINDOW} cutoff.
     */
    private Clock clock = Clock.systemUTC();

    /** Test-only seam — let unit tests inject a fixed clock. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LandingStatsResponse stats() {
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minus(MOMENTUM_WINDOW);
        return new LandingStatsResponse(
                properties.countByStatus(PropertyStatus.LISTED),
                users.countByActiveRole(Role.USER),
                properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED),
                users.countByActiveRoleAndCreatedAtAfter(Role.USER, cutoff)
        );
    }

    /** Hard cap on {@code GET /landing/cities?size=}. */
    static final int POPULAR_CITIES_MAX_SIZE = 20;

    /** Floor for the same param — negative / zero get coerced to this. */
    static final int POPULAR_CITIES_MIN_SIZE = 1;

    /**
     * Cities with the most LISTED properties, capped at {@code size}.
     * Returns an editorial fallback ({@link LandingContent#EDITORIAL_CITIES}
     * with {@code listingCount = 0}) when no real listings exist yet so
     * the "Trusted across" marquee never renders empty.
     */
    @Transactional(readOnly = true)
    public List<PopularCityResponse> popularCities(int size) {
        int safeSize = Math.min(POPULAR_CITIES_MAX_SIZE, Math.max(POPULAR_CITIES_MIN_SIZE, size));
        List<PopularCityResponse> live = properties.findPopularCities(
                PropertyStatus.LISTED, PageRequest.of(0, safeSize));
        if (!live.isEmpty()) {
            return live;
        }
        return LandingContent.EDITORIAL_CITIES.stream()
                .limit(safeSize)
                .map(name -> new PopularCityResponse(name, 0L))
                .toList();
    }
}
