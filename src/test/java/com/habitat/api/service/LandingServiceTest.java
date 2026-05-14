package com.habitat.api.service;

import com.habitat.api.dto.landing.LandingStatsResponse;
import com.habitat.api.dto.landing.PopularCityResponse;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.Role;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandingServiceTest {

    @Mock PropertyRepository properties;
    @Mock UserRepository users;
    @InjectMocks LandingService service;

    /** Fixed instant so tests can assert against an exact cutoff. */
    private static final Instant FROZEN_NOW = Instant.parse("2026-05-14T12:00:00Z");
    private static final OffsetDateTime EXPECTED_CUTOFF =
            OffsetDateTime.parse("2026-05-07T12:00:00Z");

    @BeforeEach
    void freezeClock() {
        service.setClock(Clock.fixed(FROZEN_NOW, ZoneOffset.UTC));
    }

    @Test
    void stats_aggregates_four_counts_into_one_payload() {
        when(properties.countByStatus(PropertyStatus.LISTED)).thenReturn(20L);
        when(users.countByActiveRole(Role.USER)).thenReturn(7L);
        when(properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED)).thenReturn(11L);
        when(users.countByActiveRoleAndCreatedAtAfter(eq(Role.USER), any())).thenReturn(3L);

        LandingStatsResponse out = service.stats();

        assertThat(out.activeListings()).isEqualTo(20L);
        assertThat(out.registeredTenants()).isEqualTo(7L);
        assertThat(out.suburbsCovered()).isEqualTo(11L);
        assertThat(out.tenantsLast7Days()).isEqualTo(3L);
    }

    @Test
    void stats_returns_zeros_when_catalogue_empty() {
        when(properties.countByStatus(PropertyStatus.LISTED)).thenReturn(0L);
        when(users.countByActiveRole(Role.USER)).thenReturn(0L);
        when(properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED)).thenReturn(0L);
        when(users.countByActiveRoleAndCreatedAtAfter(eq(Role.USER), any())).thenReturn(0L);

        LandingStatsResponse out = service.stats();

        assertThat(out).isEqualTo(new LandingStatsResponse(0L, 0L, 0L, 0L));
    }

    @Test
    void stats_only_counts_LISTED_properties_not_DRAFT_or_UNLISTED() {
        when(properties.countByStatus(PropertyStatus.LISTED)).thenReturn(5L);
        when(users.countByActiveRole(Role.USER)).thenReturn(3L);
        when(properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED)).thenReturn(2L);
        when(users.countByActiveRoleAndCreatedAtAfter(eq(Role.USER), any())).thenReturn(1L);

        service.stats();

        // PropertyStatus enum has DRAFT/LISTED/UNLISTED — confirm the
        // service never queries the other two states.
        verify(properties).countByStatus(PropertyStatus.LISTED);
        verify(properties).countDistinctSuburbsByStatus(PropertyStatus.LISTED);
        verify(users).countByActiveRole(Role.USER);
        verify(users).countByActiveRoleAndCreatedAtAfter(eq(Role.USER), any());
        verifyNoMoreInteractions(properties, users);
    }

    // ── popularCities ──────────────────────────────────────────────────

    @Test
    void popularCities_returns_live_ranking_when_repo_has_data() {
        List<PopularCityResponse> ranked = List.of(
                new PopularCityResponse("Johannesburg", 12L),
                new PopularCityResponse("Cape Town", 5L),
                new PopularCityResponse("Durban", 3L)
        );
        when(properties.findPopularCities(eq(PropertyStatus.LISTED), any(Pageable.class)))
                .thenReturn(ranked);

        List<PopularCityResponse> out = service.popularCities(7);

        assertThat(out).containsExactlyElementsOf(ranked);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(properties).findPopularCities(eq(PropertyStatus.LISTED), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(7);
    }

    @Test
    void popularCities_falls_back_to_editorial_list_when_repo_empty() {
        when(properties.findPopularCities(any(), any())).thenReturn(List.of());

        List<PopularCityResponse> out = service.popularCities(7);

        // Editorial list is Johannesburg / Cape Town / Durban / Pretoria /
        // Gqeberha / Polokwane / Bloemfontein — the same hardcoded strip
        // habitat shipped before the endpoint went live.
        assertThat(out).extracting(PopularCityResponse::name)
                .containsExactly("Johannesburg", "Cape Town", "Durban",
                        "Pretoria", "Gqeberha", "Polokwane", "Bloemfontein");
        assertThat(out).extracting(PopularCityResponse::listingCount)
                .containsOnly(0L);
    }

    @Test
    void popularCities_caps_size_at_20_and_floors_at_1() {
        when(properties.findPopularCities(any(), any())).thenReturn(List.of());

        service.popularCities(500);
        service.popularCities(0);
        service.popularCities(-3);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(properties, org.mockito.Mockito.times(3))
                .findPopularCities(eq(PropertyStatus.LISTED), pageable.capture());
        assertThat(pageable.getAllValues()).extracting(Pageable::getPageSize)
                .containsExactly(20, 1, 1);
    }

    @Test
    void stats_cutoff_is_exactly_seven_days_before_now() {
        when(properties.countByStatus(PropertyStatus.LISTED)).thenReturn(0L);
        when(users.countByActiveRole(Role.USER)).thenReturn(0L);
        when(properties.countDistinctSuburbsByStatus(PropertyStatus.LISTED)).thenReturn(0L);
        when(users.countByActiveRoleAndCreatedAtAfter(eq(Role.USER), any())).thenReturn(0L);

        service.stats();

        ArgumentCaptor<OffsetDateTime> cutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(users).countByActiveRoleAndCreatedAtAfter(eq(Role.USER), cutoff.capture());
        // Cutoff must be exactly 7 days before the frozen "now" — drift here
        // would mean the "This week" card silently widens or narrows its
        // window as we change clocks.
        assertThat(cutoff.getValue()).isEqualTo(EXPECTED_CUTOFF);
    }
}
