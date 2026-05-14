package com.habitat.api.service;

import com.habitat.api.dto.landing.LandingStatsResponse;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.Role;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final PropertyRepository properties;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public LandingStatsResponse stats() {
        return new LandingStatsResponse(
                properties.countByStatus(PropertyStatus.LISTED),
                users.countByActiveRole(Role.USER),
                properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED)
        );
    }
}
