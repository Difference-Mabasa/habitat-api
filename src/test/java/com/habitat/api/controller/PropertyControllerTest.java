package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.property.CreatePropertyRequest;
import com.habitat.api.dto.property.CreateUnitRequest;
import com.habitat.api.dto.property.PopularAreaResponse;
import com.habitat.api.dto.property.PropertyDetailResponse;
import com.habitat.api.dto.property.PropertySummary;
import com.habitat.api.dto.property.UnitResponse;
import com.habitat.api.dto.property.UpdatePropertyRequest;
import com.habitat.api.enums.PaymentFrequency;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.enums.PropertyType;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.enums.UnitType;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.PropertyImageService;
import com.habitat.api.service.PropertyService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PropertyControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean PropertyService properties;
    @MockBean PropertyImageService propertyImages;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID PROP_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID UNIT_ID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    @Test
    void search_returns_paged_summaries() throws Exception {
        PropertySummary summary = new PropertySummary(
                PROP_ID, "Sandton Villa", "Sandton", "Johannesburg", "Gauteng",
                -26.1, 28.0, PropertyType.HOUSE, "https://img/1.jpg",
                UNIT_ID, UnitType.HOUSE, new BigDecimal("45000"), 4, 3, 320,
                1, 1, OffsetDateTime.parse("2026-05-01T08:00:00Z"),
                new BigDecimal("4.50"), 12
        );
        PageResponse<PropertySummary> page = PageResponse.<PropertySummary>builder()
                .content(List.of(summary)).page(0).size(20).totalElements(1L).totalPages(1).build();
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any(), eq(0), eq(20))).thenReturn(page);

        mvc.perform(get(ApiRoutes.PROPERTIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Sandton Villa"))
                .andExpect(jsonPath("$.content[0].headlinePrice").value(45000));
    }

    @Test
    void search_forwards_query_params() throws Exception {
        when(properties.search(any(), any(), any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(PageResponse.<PropertySummary>builder()
                        .content(List.of()).page(0).size(10).totalElements(0L).totalPages(0).build());

        mvc.perform(get(ApiRoutes.PROPERTIES)
                        .param("location", "Sandton,Camps Bay")
                        .param("type", "APARTMENT,STUDIO")
                        .param("minPrice", "5000")
                        .param("maxPrice", "20000")
                        .param("minBeds", "2")
                        .param("minSqm", "60")
                        .param("sort", "PRICE")
                        .param("dir", "ASC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(properties).search(
                eq(List.of("Sandton", "Camps Bay")),
                eq(List.of(UnitType.APARTMENT, UnitType.STUDIO)),
                eq(new BigDecimal("5000")),
                eq(new BigDecimal("20000")),
                eq(2),
                eq(60),
                eq(PropertyService.SortKey.PRICE),
                eq(PropertyService.SortDirection.ASC),
                eq(0),
                eq(10)
        );
    }

    @Test
    void getById_returns_detail() throws Exception {
        when(properties.getById(PROP_ID)).thenReturn(sampleDetail());

        mvc.perform(get(ApiRoutes.PROPERTIES + "/" + PROP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROP_ID.toString()))
                .andExpect(jsonPath("$.status").value("LISTED"));
    }

    @Test
    void create_returns_201() throws Exception {
        CreatePropertyRequest req = new CreatePropertyRequest(
                "Sandton Villa", null, PropertyType.HOUSE,
                null, "Sandton", "Johannesburg", "Gauteng", "2196",
                null, null,
                null, null
        );
        when(properties.create(any(CreatePropertyRequest.class))).thenReturn(sampleDetail());

        mvc.perform(post(ApiRoutes.PROPERTIES).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROP_ID.toString()));
    }

    @Test
    void create_returns_422_on_blank_title() throws Exception {
        CreatePropertyRequest req = new CreatePropertyRequest(
                "", null, PropertyType.HOUSE,
                null, null, null, null, null, null, null,
                null, null
        );

        mvc.perform(post(ApiRoutes.PROPERTIES).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void update_returns_200() throws Exception {
        UpdatePropertyRequest req = new UpdatePropertyRequest(
                "New Title", null, null, PropertyStatus.LISTED,
                null, null, null, null, null, null, null,
                null, null
        );
        when(properties.update(eq(PROP_ID), any(UpdatePropertyRequest.class))).thenReturn(sampleDetail());

        mvc.perform(patch(ApiRoutes.PROPERTIES + "/" + PROP_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns_204() throws Exception {
        mvc.perform(delete(ApiRoutes.PROPERTIES + "/" + PROP_ID).with(csrf()))
                .andExpect(status().isNoContent());
        verify(properties).delete(eq(PROP_ID));
    }

    @Test
    void addUnit_returns_201() throws Exception {
        CreateUnitRequest req = new CreateUnitRequest(
                "A1", "Unit A", null, UnitType.APARTMENT, null,
                new BigDecimal("15000"), null, null, 2, 1, 90, 4,
                null, null, null, null
        );
        when(properties.addUnit(eq(PROP_ID), any(CreateUnitRequest.class))).thenReturn(sampleUnit());

        mvc.perform(post(ApiRoutes.PROPERTIES + "/" + PROP_ID + "/units").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(UNIT_ID.toString()));
    }

    @Test
    void popular_areas_returns_payload_with_default_size() throws Exception {
        when(properties.popularAreas(3)).thenReturn(List.of(
                new PopularAreaResponse("Sandton", 5L),
                new PopularAreaResponse("Camps Bay", 3L),
                new PopularAreaResponse("Umhlanga", 1L)
        ));

        mvc.perform(get(ApiRoutes.PROPERTIES_POPULAR_AREAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sandton"))
                .andExpect(jsonPath("$[0].listingCount").value(5))
                .andExpect(jsonPath("$[2].name").value("Umhlanga"));

        verify(properties).popularAreas(3);
    }

    @Test
    void popular_areas_forwards_size_query_param() throws Exception {
        when(properties.popularAreas(6)).thenReturn(List.of());

        mvc.perform(get(ApiRoutes.PROPERTIES_POPULAR_AREAS).param("size", "6"))
                .andExpect(status().isOk());

        verify(properties).popularAreas(6);
    }

    @Test
    void top_rated_returns_summary_payload_with_default_size() throws Exception {
        PropertySummary summary = new PropertySummary(
                PROP_ID, "Sandton Villa", "Sandton", "Johannesburg", "Gauteng",
                -26.1, 28.0, PropertyType.HOUSE, "https://img/1.jpg",
                UNIT_ID, UnitType.HOUSE, new BigDecimal("45000"), 4, 3, 320,
                1, 1, OffsetDateTime.parse("2026-05-01T08:00:00Z"),
                new BigDecimal("4.80"), 12
        );
        when(properties.topRated(4)).thenReturn(List.of(summary));

        mvc.perform(get(ApiRoutes.PROPERTIES_TOP_RATED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Sandton Villa"))
                .andExpect(jsonPath("$[0].avgRating").value(4.80))
                .andExpect(jsonPath("$[0].ratingCount").value(12));

        verify(properties).topRated(4);
    }

    @Test
    void top_rated_forwards_size_query_param() throws Exception {
        when(properties.topRated(8)).thenReturn(List.of());

        mvc.perform(get(ApiRoutes.PROPERTIES_TOP_RATED).param("size", "8"))
                .andExpect(status().isOk());

        verify(properties).topRated(8);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static PropertyDetailResponse sampleDetail() {
        UUID landlordId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        return new PropertyDetailResponse(
                PROP_ID, "Sandton Villa", null, PropertyType.HOUSE, PropertyStatus.LISTED,
                "5 Rivonia Rd", "Sandton", "Johannesburg", "Gauteng", "2196",
                -26.1, 28.0,
                landlordId,
                landlordId,
                new com.habitat.api.dto.property.ManagerRef(landlordId, "Demo", "Owner"),
                List.of(), List.of(), OffsetDateTime.now(),
                new BigDecimal("4.50"), 12,
                com.habitat.api.enums.ListingMode.LANDLORD_DIRECT,
                null,
                List.of()
        );
    }

    private static UnitResponse sampleUnit() {
        return new UnitResponse(
                UNIT_ID, PROP_ID, "A1", "Unit A", null, UnitType.APARTMENT, UnitStatus.AVAILABLE,
                null, new BigDecimal("15000"), PaymentFrequency.MONTHLY, null,
                2, 1, 90, 4, false, false, false, null, List.of()
        );
    }
}
