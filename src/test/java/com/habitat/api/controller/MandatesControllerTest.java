package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.PageResponse;
import com.habitat.api.dto.landlord.LandlordRef;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.MandateService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MandatesController.class)
@AutoConfigureMockMvc(addFilters = false)
class MandatesControllerTest {

    @Autowired MockMvc mvc;
    @MockBean MandateService mandates;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID MANDATE_ID = UUID.fromString("ffffffff-eeee-dddd-cccc-bbbbbbbbbbbb");
    private static final UUID PROP_ID    = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID AGENT_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LANDLORD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OWNER_USER  = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void awaiting_my_approval_returns_paged_response() throws Exception {
        MandateResponse row = sampleResponse();
        PageResponse<MandateResponse> page = PageResponse.<MandateResponse>builder()
                .content(List.of(row)).page(0).size(20).totalElements(1L).totalPages(1).build();
        when(mandates.listAwaitingMyApproval(0, 20)).thenReturn(page);

        mvc.perform(get(ApiRoutes.MANDATES_AWAITING_MY_APPROVAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(MANDATE_ID.toString()))
                .andExpect(jsonPath("$.content[0].propertyId").value(PROP_ID.toString()))
                .andExpect(jsonPath("$.content[0].agentFirstName").value("Naledi"))
                .andExpect(jsonPath("$.content[0].agentSurname").value("M."))
                .andExpect(jsonPath("$.content[0].propertyTitle").value("Sandton Villa"))
                .andExpect(jsonPath("$.content[0].propertySuburb").value("Sandton"))
                .andExpect(jsonPath("$.content[0].mandateType").value("FULL_MANAGEMENT"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING_LANDLORD_APPROVAL"))
                .andExpect(jsonPath("$.content[0].signedName").value("Thandi Mokoena"))
                .andExpect(jsonPath("$.content[0].signedAt").value("2026-05-28T15:00:00Z"));
    }

    @Test
    void awaiting_my_approval_forwards_pagination_params() throws Exception {
        when(mandates.listAwaitingMyApproval(2, 5)).thenReturn(
                PageResponse.<MandateResponse>builder()
                        .content(List.of()).page(2).size(5).totalElements(11L).totalPages(3).build());

        mvc.perform(get(ApiRoutes.MANDATES_AWAITING_MY_APPROVAL)
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(11));

        verify(mandates).listAwaitingMyApproval(eq(2), eq(5));
    }

    @Test
    void awaiting_my_approval_defaults_page_zero_size_twenty() throws Exception {
        when(mandates.listAwaitingMyApproval(0, 20)).thenReturn(
                PageResponse.<MandateResponse>builder()
                        .content(List.of()).page(0).size(20).totalElements(0L).totalPages(0).build());

        mvc.perform(get(ApiRoutes.MANDATES_AWAITING_MY_APPROVAL))
                .andExpect(status().isOk());

        verify(mandates).listAwaitingMyApproval(eq(0), eq(20));
    }

    @Test
    void mine_returns_paged_response_and_forwards_status_filter() throws Exception {
        when(mandates.listMineAsAgent(MandateStatus.CHANGES_REQUESTED, 0, 20)).thenReturn(
                PageResponse.<MandateResponse>builder()
                        .content(List.of(sampleResponse()))
                        .page(0).size(20).totalElements(1L).totalPages(1).build());

        mvc.perform(get(ApiRoutes.MANDATES + "/mine")
                        .param("status", "CHANGES_REQUESTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(MANDATE_ID.toString()));

        verify(mandates).listMineAsAgent(eq(MandateStatus.CHANGES_REQUESTED), eq(0), eq(20));
    }

    @Test
    void mine_omits_status_filter_when_not_provided() throws Exception {
        when(mandates.listMineAsAgent(null, 0, 20)).thenReturn(
                PageResponse.<MandateResponse>builder()
                        .content(List.of()).page(0).size(20).totalElements(0L).totalPages(0).build());

        mvc.perform(get(ApiRoutes.MANDATES + "/mine"))
                .andExpect(status().isOk());

        verify(mandates).listMineAsAgent(eq((MandateStatus) null), eq(0), eq(20));
    }

    private static MandateResponse sampleResponse() {
        LandlordRef landlord = new LandlordRef(
                LANDLORD_ID, LandlordType.ONLINE, OWNER_USER,
                "Thandi", "Mokoena", "thandi@example.co.za", null, null);
        return new MandateResponse(
                MANDATE_ID,
                PROP_ID,
                AGENT_ID,
                landlord,
                "Naledi",
                "M.",
                "Sandton Villa",
                "Sandton",
                MandateType.FULL_MANAGEMENT,
                MandateStatus.PENDING_LANDLORD_APPROVAL,
                false,
                new BigDecimal("8.0"),
                "/api/v1/properties/" + PROP_ID + "/mandate/pdf",
                null,
                "Verifying.",
                null,
                OffsetDateTime.parse("2026-05-28T13:26:27Z"),
                "Thandi Mokoena",
                OffsetDateTime.parse("2026-05-28T15:00:00Z"),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
