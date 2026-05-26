package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.lease.DeclineLeaseRequest;
import com.habitat.api.dto.lease.LeaseResponse;
import com.habitat.api.dto.lease.SignLeaseRequest;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.LeaseStatus;
import com.habitat.api.enums.LeaseTemplate;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.LeaseService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LeaseController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaseControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean LeaseService leases;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID LEASE_ID  = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID APP_ID    = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID TENANT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID LL_ID     = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID UNIT_ID   = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID PROP_ID   = UUID.fromString("33333333-4444-5555-6666-777777777777");

    @Test
    void listMine_returns_caller_leases() throws Exception {
        when(leases.listForTenant()).thenReturn(List.of(stub(LeaseStatus.PENDING_SIGNATURES)));

        mvc.perform(get(ApiRoutes.LEASES + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leaseRef").value("HB-LSE-DEMOREF"))
                .andExpect(jsonPath("$[0].status").value("PENDING_SIGNATURES"));
        verify(leases).listForTenant();
    }

    @Test
    void get_returns_single_lease() throws Exception {
        when(leases.getById(LEASE_ID)).thenReturn(stub(LeaseStatus.PENDING_SIGNATURES));

        mvc.perform(get(ApiRoutes.LEASES + "/" + LEASE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LEASE_ID.toString()));
        verify(leases).getById(LEASE_ID);
    }

    @Test
    void sign_passes_otp_through() throws Exception {
        when(leases.sign(eq(LEASE_ID), any())).thenReturn(stub(LeaseStatus.SIGNED));

        mvc.perform(post(ApiRoutes.LEASES + "/" + LEASE_ID + "/sign").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SignLeaseRequest("123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"));
        verify(leases).sign(eq(LEASE_ID), any());
    }

    @Test
    void decline_records_reason() throws Exception {
        when(leases.decline(eq(LEASE_ID), any())).thenReturn(stub(LeaseStatus.DECLINED));

        mvc.perform(post(ApiRoutes.LEASES + "/" + LEASE_ID + "/decline").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DeclineLeaseRequest("Changed mind"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
        verify(leases).decline(eq(LEASE_ID), any());
    }

    @Test
    void sign_rejects_a_missing_body() throws Exception {
        // @NotBlank otp means the controller's @Valid catches this
        // before the service is called.
        mvc.perform(post(ApiRoutes.LEASES + "/" + LEASE_ID + "/sign").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void issue_otp_returns_a_dev_code() throws Exception {
        when(leases.issueSignOtp(LEASE_ID))
                .thenReturn(new com.habitat.api.dto.lease.IssueOtpResponse("987654"));

        mvc.perform(post(ApiRoutes.LEASES + "/" + LEASE_ID + "/otp").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devCode").value("987654"));
        verify(leases).issueSignOtp(LEASE_ID);
    }

    @Test
    void download_pdf_streams_the_signed_file() throws Exception {
        byte[] payload = "%PDF-1.4 fake".getBytes();
        when(leases.openSignedPdf(LEASE_ID)).thenReturn(
                new com.habitat.api.service.LeaseService.PdfHandle(
                        new com.habitat.api.storage.StoredResource(
                                new java.io.ByteArrayInputStream(payload),
                                "application/pdf", payload.length),
                        "HB-LSE-DEMOREF.pdf"));

        mvc.perform(get(ApiRoutes.LEASES + "/" + LEASE_ID + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Type", "application/pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Disposition",
                                "inline; filename=\"HB-LSE-DEMOREF.pdf\""));
    }

    private static LeaseResponse stub(LeaseStatus status) {
        return new LeaseResponse(
                LEASE_ID,
                "HB-LSE-DEMOREF",
                APP_ID,
                status,
                LeaseTemplate.RHA_STANDARD,
                new BigDecimal("8500"),
                new BigDecimal("8500"),
                12,
                LocalDate.parse("2026-08-01"),
                null,
                null,
                null,
                OffsetDateTime.parse("2026-05-14T10:00:00Z"),
                new LeaseResponse.ApplicationRef(APP_ID, ApplicationStatus.LEASE_PENDING_SIGNATURES),
                new LeaseResponse.PartyRef(TENANT_ID, "Sipho", "Khumalo", "t@example.co.za"),
                new LeaseResponse.PartyRef(LL_ID, "Naledi", "Mokoena", "l@example.co.za"),
                new LeaseResponse.UnitRef(UNIT_ID, "Unit A", "A",
                        new BigDecimal("8500"), 1, 1, null),
                new LeaseResponse.PropertyRef(PROP_ID, "23 Vilakazi St", "23 Vilakazi", "Orlando West", "Soweto", "GP", "1804"),
                new LeaseResponse.Snapshots(
                        "Sipho Khumalo",
                        "Naledi Mokoena",
                        "Unit A",
                        "23 Vilakazi St",
                        "23 Vilakazi, Orlando West, Soweto, 1804")
        );
    }
}
