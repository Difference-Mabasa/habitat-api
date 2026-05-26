package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.property.CreatePropertyRequest;
import com.habitat.api.dto.property.CreateUnitRequest;
import com.habitat.api.dto.property.PopularAreaResponse;
import com.habitat.api.dto.property.PropertyDetailResponse;
import com.habitat.api.dto.property.PropertySummary;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdatePropertyRequest;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.PropertyType;
import com.habitat.api.enums.Role;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.repository.UnitRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock PropertyRepository properties;
    @Mock UnitRepository units;
    @Mock UserRepository users;
    @Mock com.habitat.api.repository.PropertyRequiredDocumentRepository requiredDocs;
    @Mock com.habitat.api.repository.AmenityRepository amenities;
    @Mock SecurityUtils security;
    @InjectMocks PropertyService service;

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PROP_ID  = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    // ── search ─────────────────────────────────────────────────────────

    @Test
    void search_returns_mapped_page_of_summaries() {
        Property p = propertyWith("Sandton Villa", PropertyStatus.LISTED, "Sandton");
        attachUnit(p, new BigDecimal("45000"), 4, 3, UnitStatus.AVAILABLE);
        Page<Property> page = new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1);
        when(properties.search(eq(PropertyStatus.LISTED), eq(""), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<PropertySummary> out = service.search(null, null, null, null, null, null, null, null, 0, 20);

        assertThat(out.totalElements()).isEqualTo(1);
        assertThat(out.content()).hasSize(1);
        assertThat(out.content().get(0).title()).isEqualTo("Sandton Villa");
        assertThat(out.content().get(0).headlinePrice()).isEqualByComparingTo("45000");
    }

    @Test
    void search_passes_empty_unit_types_as_null() {
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.search(null, List.of(), null, null, null, null, null, null, 0, 20);

        verify(properties).search(eq(PropertyStatus.LISTED), eq(""), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void search_forwards_unit_filters_to_sql_and_skips_location() {
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.search(List.of("Sandton"), List.of(UnitType.APARTMENT, UnitType.STUDIO),
                new BigDecimal("5000"), new BigDecimal("20000"), 2, 60, null, null, 1, 10);

        // Locations are matched in Java — SQL always sees empty location.
        verify(properties).search(
                eq(PropertyStatus.LISTED),
                eq(""),
                eq(List.of(UnitType.APARTMENT, UnitType.STUDIO)),
                eq(new BigDecimal("5000")),
                eq(new BigDecimal("20000")),
                eq(2),
                eq(60),
                any(Pageable.class)
        );
    }

    @Test
    void search_filters_by_any_location_match_case_insensitive() {
        Property sandton = propertyWith("Sandton House", PropertyStatus.LISTED, "Sandton");
        attachUnit(sandton, new BigDecimal("45000"), 4, 3, UnitStatus.AVAILABLE);
        Property campsBay = propertyWith("Camps Bay Villa", PropertyStatus.LISTED, "Camps Bay");
        attachUnit(campsBay, new BigDecimal("65000"), 4, 4, UnitStatus.AVAILABLE);
        Property morningside = propertyWith("Morningside Townhouse", PropertyStatus.LISTED, "Morningside");
        attachUnit(morningside, new BigDecimal("28000"), 3, 2, UnitStatus.AVAILABLE);
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sandton, campsBay, morningside)));

        PageResponse<PropertySummary> out = service.search(
                List.of("sandt", "camps"), null, null, null, null, null, null, null, 0, 20
        );

        assertThat(out.totalElements()).isEqualTo(2);
        assertThat(out.content()).extracting(PropertySummary::title)
                .containsExactlyInAnyOrder("Sandton House", "Camps Bay Villa");
    }

    @Test
    void search_paginates_filtered_results() {
        Property p1 = propertyWith("Sandton 1", PropertyStatus.LISTED, "Sandton");
        attachUnit(p1, new BigDecimal("45000"), 4, 3, UnitStatus.AVAILABLE);
        Property p2 = propertyWith("Sandton 2", PropertyStatus.LISTED, "Sandton");
        attachUnit(p2, new BigDecimal("38000"), 3, 2, UnitStatus.AVAILABLE);
        Property p3 = propertyWith("Sandton 3", PropertyStatus.LISTED, "Sandton");
        attachUnit(p3, new BigDecimal("28000"), 2, 2, UnitStatus.AVAILABLE);
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p1, p2, p3)));

        PageResponse<PropertySummary> page1 = service.search(null, null, null, null, null, null, null, null, 0, 2);
        assertThat(page1.content()).hasSize(2);
        assertThat(page1.totalElements()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);

        PageResponse<PropertySummary> page2 = service.search(null, null, null, null, null, null, null, null, 1, 2);
        assertThat(page2.content()).hasSize(1);
    }

    // ── sort ──────────────────────────────────────────────────────────

    @Test
    void search_sorts_by_price_ascending() {
        Property p1 = propertyWith("High", PropertyStatus.LISTED, "Sandton");
        attachUnit(p1, new BigDecimal("45000"), 4, 3, UnitStatus.AVAILABLE);
        Property p2 = propertyWith("Mid", PropertyStatus.LISTED, "Sandton");
        attachUnit(p2, new BigDecimal("28000"), 3, 2, UnitStatus.AVAILABLE);
        Property p3 = propertyWith("Low", PropertyStatus.LISTED, "Sandton");
        attachUnit(p3, new BigDecimal("12000"), 1, 1, UnitStatus.AVAILABLE);
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p1, p2, p3)));

        PageResponse<PropertySummary> out = service.search(
                null, null, null, null, null, null,
                PropertyService.SortKey.PRICE, PropertyService.SortDirection.ASC,
                0, 20
        );

        assertThat(out.content()).extracting(PropertySummary::title)
                .containsExactly("Low", "Mid", "High");
    }

    @Test
    void search_sorts_by_bedrooms_descending() {
        Property a = propertyWith("4 bed", PropertyStatus.LISTED, "Sandton");
        attachUnit(a, new BigDecimal("45000"), 4, 3, UnitStatus.AVAILABLE);
        Property b = propertyWith("2 bed", PropertyStatus.LISTED, "Sandton");
        attachUnit(b, new BigDecimal("28000"), 2, 2, UnitStatus.AVAILABLE);
        Property c = propertyWith("3 bed", PropertyStatus.LISTED, "Sandton");
        attachUnit(c, new BigDecimal("32000"), 3, 2, UnitStatus.AVAILABLE);
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(a, b, c)));

        PageResponse<PropertySummary> out = service.search(
                null, null, null, null, null, null,
                PropertyService.SortKey.BEDROOMS, PropertyService.SortDirection.DESC,
                0, 20
        );

        assertThat(out.content()).extracting(PropertySummary::title)
                .containsExactly("4 bed", "3 bed", "2 bed");
    }

    @Test
    void search_default_sort_is_newest_first() {
        Property p1 = propertyWith("First", PropertyStatus.LISTED, "Sandton");
        attachUnit(p1, new BigDecimal("12000"), 1, 1, UnitStatus.AVAILABLE);
        // Repository returns rows in the order the SQL ORDER BY produced;
        // the default sort key NEWEST + DESC is a no-op pass-through, so the
        // service-level sort needs to preserve that incoming order.
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p1)));

        PageResponse<PropertySummary> out = service.search(null, null, null, null, null, null, null, null, 0, 20);
        assertThat(out.content()).hasSize(1);
    }

    // ── popularAreas ──────────────────────────────────────────────────

    @Test
    void popularAreas_returns_live_ranking_when_repo_has_data() {
        List<PopularAreaResponse> ranked = List.of(
                new PopularAreaResponse("Sandton", 7L),
                new PopularAreaResponse("Camps Bay", 4L),
                new PopularAreaResponse("Umhlanga", 2L)
        );
        when(properties.findPopularSuburbs(eq(PropertyStatus.LISTED), any(Pageable.class)))
                .thenReturn(ranked);

        List<PopularAreaResponse> out = service.popularAreas(3);

        assertThat(out).containsExactlyElementsOf(ranked);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(properties).findPopularSuburbs(eq(PropertyStatus.LISTED), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
        assertThat(pageable.getValue().getPageNumber()).isZero();
    }

    @Test
    void popularAreas_falls_back_to_editorial_list_when_repo_empty() {
        when(properties.findPopularSuburbs(any(), any())).thenReturn(List.of());

        List<PopularAreaResponse> out = service.popularAreas(3);

        assertThat(out).extracting(PopularAreaResponse::name)
                .containsExactly("Sandton", "Umhlanga", "Camps Bay");
        assertThat(out).extracting(PopularAreaResponse::listingCount)
                .containsOnly(0L);
    }

    @Test
    void popularAreas_caps_size_at_20_and_floors_at_1() {
        when(properties.findPopularSuburbs(any(), any())).thenReturn(List.of());

        service.popularAreas(500);
        service.popularAreas(0);
        service.popularAreas(-3);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(properties, org.mockito.Mockito.times(3))
                .findPopularSuburbs(eq(PropertyStatus.LISTED), pageable.capture());
        assertThat(pageable.getAllValues()).extracting(Pageable::getPageSize)
                .containsExactly(20, 1, 1);
    }

    @Test
    void popularAreas_editorial_fallback_respects_requested_size() {
        when(properties.findPopularSuburbs(any(), any())).thenReturn(List.of());

        List<PopularAreaResponse> out = service.popularAreas(2);

        assertThat(out).hasSize(2);
        assertThat(out).extracting(PopularAreaResponse::name)
                .containsExactly("Sandton", "Umhlanga");
    }

    // ── topRated ──────────────────────────────────────────────────────

    @Test
    void topRated_passes_size_through_to_repo_and_maps_to_summaries() {
        Property a = propertyWith("Sandton Villa", PropertyStatus.LISTED, "Sandton");
        attachUnit(a, new BigDecimal("45000"), 4, 3, UnitStatus.AVAILABLE);
        Property b = propertyWith("Camps Bay House", PropertyStatus.LISTED, "Camps Bay");
        attachUnit(b, new BigDecimal("65000"), 4, 4, UnitStatus.AVAILABLE);
        when(properties.findTopRated(eq(PropertyStatus.LISTED), any(Pageable.class)))
                .thenReturn(List.of(a, b));

        List<PropertySummary> out = service.topRated(4);

        assertThat(out).extracting(PropertySummary::title)
                .containsExactly("Sandton Villa", "Camps Bay House");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(properties).findTopRated(eq(PropertyStatus.LISTED), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(4);
    }

    @Test
    void topRated_caps_size_at_20_and_floors_at_1() {
        when(properties.findTopRated(any(), any())).thenReturn(List.of());

        service.topRated(500);
        service.topRated(0);
        service.topRated(-3);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(properties, org.mockito.Mockito.times(3))
                .findTopRated(eq(PropertyStatus.LISTED), pageable.capture());
        assertThat(pageable.getAllValues()).extracting(Pageable::getPageSize)
                .containsExactly(20, 1, 1);
    }

    // ── getById ───────────────────────────────────────────────────────

    @Test
    void getById_returns_listed_property() {
        Property p = propertyWith("Sandton", PropertyStatus.LISTED);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));

        PropertyDetailResponse out = service.getById(PROP_ID);
        assertThat(out.id()).isEqualTo(PROP_ID);
        assertThat(out.status()).isEqualTo(PropertyStatus.LISTED);
    }

    @Test
    void getById_throws_when_draft_and_caller_not_owner() {
        Property p = propertyWith("Sandton", PropertyStatus.DRAFT);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OTHER_ID));

        assertThatThrownBy(() -> service.getById(PROP_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ErrorMessages.PROPERTY_NOT_FOUND);
    }

    @Test
    void getById_returns_draft_when_caller_is_owner() {
        Property p = propertyWith("Sandton", PropertyStatus.DRAFT);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OWNER_ID));

        PropertyDetailResponse out = service.getById(PROP_ID);
        assertThat(out.status()).isEqualTo(PropertyStatus.DRAFT);
    }

    @Test
    void getById_returns_draft_when_caller_is_admin() {
        Property p = propertyWith("Sandton", PropertyStatus.DRAFT);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(true);

        PropertyDetailResponse out = service.getById(PROP_ID);
        assertThat(out.status()).isEqualTo(PropertyStatus.DRAFT);
    }

    @Test
    void getById_throws_when_property_missing() {
        when(properties.findById(PROP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(PROP_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────

    @Test
    void create_persists_with_caller_as_landlord_and_manager() {
        User caller = userWithId(OWNER_ID);
        when(security.requireUserId()).thenReturn(OWNER_ID);
        when(users.findById(OWNER_ID)).thenReturn(Optional.of(caller));

        CreatePropertyRequest req = new CreatePropertyRequest(
                "Sandton Villa", "  ", PropertyType.HOUSE,
                "5 Rivonia Rd", "Sandton", "Johannesburg", "Gauteng", "2196",
                -26.1, 28.0,
                null, null
        );

        service.create(req);

        ArgumentCaptor<Property> captor = ArgumentCaptor.forClass(Property.class);
        verify(properties).save(captor.capture());
        Property saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Sandton Villa");
        assertThat(saved.getDescription()).isNull();  // blank trimmed to null
        assertThat(saved.getPropertyType()).isEqualTo(PropertyType.HOUSE);
        assertThat(saved.getStatus()).isEqualTo(PropertyStatus.DRAFT);
        assertThat(saved.getLandlord()).isSameAs(caller);
        assertThat(saved.getManager()).isSameAs(caller);
    }

    @Test
    void create_throws_when_caller_user_not_found() {
        when(security.requireUserId()).thenReturn(OWNER_ID);
        when(users.findById(OWNER_ID)).thenReturn(Optional.empty());

        CreatePropertyRequest req = new CreatePropertyRequest(
                "Sandton Villa", null, PropertyType.HOUSE,
                null, null, null, null, null, null, null,
                null, null
        );

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────

    @Test
    void update_applies_only_non_null_fields_for_owner() {
        Property p = propertyWith("Old Title", PropertyStatus.DRAFT);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OWNER_ID));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.update(PROP_ID, new UpdatePropertyRequest(
                "New Title", null, null, PropertyStatus.LISTED,
                null, "Sandton", null, null, null, null, null,
                null, null
        ));

        assertThat(p.getTitle()).isEqualTo("New Title");
        assertThat(p.getStatus()).isEqualTo(PropertyStatus.LISTED);
        assertThat(p.getSuburb()).isEqualTo("Sandton");
        assertThat(p.getCity()).isNull();
    }

    @Test
    void update_throws_for_non_owner() {
        Property p = propertyWith("Title", PropertyStatus.LISTED);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OTHER_ID));

        assertThatThrownBy(() -> service.update(PROP_ID, new UpdatePropertyRequest(
                "x", null, null, null, null, null, null, null, null, null, null,
                null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_throws_for_unauthenticated_caller() {
        Property p = propertyWith("Title", PropertyStatus.LISTED);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(PROP_ID, new UpdatePropertyRequest(
                "x", null, null, null, null, null, null, null, null, null, null,
                null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── delete ────────────────────────────────────────────────────────

    @Test
    void delete_soft_deletes_for_owner() {
        Property p = propertyWith("Title", PropertyStatus.LISTED);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OWNER_ID));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        service.delete(PROP_ID);

        assertThat(p.getDeletedAt()).isNotNull();
    }

    @Test
    void delete_throws_for_non_owner() {
        Property p = propertyWith("Title", PropertyStatus.LISTED);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OTHER_ID));

        assertThatThrownBy(() -> service.delete(PROP_ID))
                .isInstanceOf(ForbiddenException.class);
        assertThat(p.getDeletedAt()).isNull();
    }

    // ── addUnit ───────────────────────────────────────────────────────

    @Test
    void addUnit_creates_unit_with_property_link() {
        Property p = propertyWith("Title", PropertyStatus.LISTED);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OWNER_ID));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        CreateUnitRequest req = new CreateUnitRequest(
                "A1", "Unit A", "Nice unit", UnitType.APARTMENT, null,
                new BigDecimal("15000"), PaymentFrequency.MONTHLY,
                new BigDecimal("30000"), 2, 1, 90, 4,
                true, false, false, null
        );

        UnitResponse out = service.addUnit(PROP_ID, req);

        ArgumentCaptor<Unit> captor = ArgumentCaptor.forClass(Unit.class);
        verify(units).save(captor.capture());
        Unit saved = captor.getValue();
        assertThat(saved.getProperty()).isSameAs(p);
        assertThat(saved.getTitle()).isEqualTo("Unit A");
        assertThat(saved.getUnitType()).isEqualTo(UnitType.APARTMENT);
        assertThat(saved.getStatus()).isEqualTo(UnitStatus.AVAILABLE);
        assertThat(out.unitType()).isEqualTo(UnitType.APARTMENT);
    }

    @Test
    void addUnit_defaults_payment_frequency_to_monthly_when_omitted() {
        Property p = propertyWith("Title", PropertyStatus.LISTED);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OWNER_ID));
        when(security.requireUserId()).thenReturn(OWNER_ID);

        CreateUnitRequest req = new CreateUnitRequest(
                null, "Unit A", null, UnitType.STUDIO, null,
                new BigDecimal("12000"), null, null,
                1, 1, null, null, null, null, null, null
        );

        service.addUnit(PROP_ID, req);

        ArgumentCaptor<Unit> captor = ArgumentCaptor.forClass(Unit.class);
        verify(units).save(captor.capture());
        assertThat(captor.getValue().getPaymentFrequency()).isEqualTo(PaymentFrequency.MONTHLY);
    }

    @Test
    void addUnit_throws_for_non_owner() {
        Property p = propertyWith("Title", PropertyStatus.LISTED);
        when(properties.findById(PROP_ID)).thenReturn(Optional.of(p));
        when(security.isPrivileged()).thenReturn(false);
        when(security.currentUserId()).thenReturn(Optional.of(OTHER_ID));

        CreateUnitRequest req = new CreateUnitRequest(
                null, "Unit", null, UnitType.STUDIO, null,
                new BigDecimal("1000"), null, null, 1, 1, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.addUnit(PROP_ID, req))
                .isInstanceOf(ForbiddenException.class);
        verify(units, never()).save(any());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Property propertyWith(String title, PropertyStatus status) {
        return propertyWith(title, status, null);
    }

    private Property propertyWith(String title, PropertyStatus status, String suburb) {
        User owner = userWithId(OWNER_ID);
        Property p = Property.builder()
                .landlord(owner)
                .manager(owner)
                .title(title)
                .propertyType(PropertyType.HOUSE)
                .status(status)
                .suburb(suburb)
                .build();
        setId(p, PROP_ID);
        return p;
    }

    private void attachUnit(Property p, BigDecimal price, int beds, int baths, UnitStatus status) {
        Unit u = Unit.builder()
                .property(p)
                .title("Unit")
                .unitType(UnitType.HOUSE)
                .status(status)
                .price(price)
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .bedrooms(beds)
                .bathrooms(baths)
                .build();
        p.getUnits().add(u);
    }

    private static User userWithId(UUID id) {
        User u = User.builder()
                .email("u@example.co.za")
                .firstName("U").surname("ser")
                .passwordHash("h")
                .roles(new HashSet<>(List.of(Role.USER)))
                .activeRole(Role.USER)
                .emailVerified(true)
                .build();
        setId(u, id);
        return u;
    }

    private static void setId(BaseEntity entity, UUID id) {
        try {
            Field f = BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
