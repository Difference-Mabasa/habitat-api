package com.habitat.api.service;

import com.habitat.api.dto.landing.LandingStatsResponse;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.Role;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandingServiceTest {

    @Mock PropertyRepository properties;
    @Mock UserRepository users;
    @InjectMocks LandingService service;

    @Test
    void stats_aggregates_three_counts_into_one_payload() {
        when(properties.countByStatus(PropertyStatus.LISTED)).thenReturn(20L);
        when(users.countByActiveRole(Role.USER)).thenReturn(7L);
        when(properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED)).thenReturn(11L);

        LandingStatsResponse out = service.stats();

        assertThat(out.activeListings()).isEqualTo(20L);
        assertThat(out.registeredTenants()).isEqualTo(7L);
        assertThat(out.suburbsCovered()).isEqualTo(11L);
    }

    @Test
    void stats_returns_zeros_when_catalogue_empty() {
        when(properties.countByStatus(PropertyStatus.LISTED)).thenReturn(0L);
        when(users.countByActiveRole(Role.USER)).thenReturn(0L);
        when(properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED)).thenReturn(0L);

        LandingStatsResponse out = service.stats();

        assertThat(out).isEqualTo(new LandingStatsResponse(0L, 0L, 0L));
    }

    @Test
    void stats_only_counts_LISTED_properties_not_DRAFT_or_UNLISTED() {
        when(properties.countByStatus(PropertyStatus.LISTED)).thenReturn(5L);
        when(users.countByActiveRole(Role.USER)).thenReturn(3L);
        when(properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED)).thenReturn(2L);

        service.stats();

        // PropertyStatus enum has DRAFT/LISTED/UNLISTED — confirm the
        // service never queries the other two states.
        verify(properties).countByStatus(PropertyStatus.LISTED);
        verify(properties).countDistinctSuburbsByStatus(PropertyStatus.LISTED);
        verify(users).countByActiveRole(Role.USER);
        verifyNoMoreInteractions(properties, users);
    }
}
