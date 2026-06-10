package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.landlord.LandlordRef;
import com.habitat.api.dto.mandate.HistoryEventResponse;
import com.habitat.api.dto.mandate.MandateHistoryResponse;
import com.habitat.api.dto.mandate.MandateResponse;
import com.habitat.api.enums.HistoryEventKind;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.MandateStatus;
import com.habitat.api.enums.MandateType;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.BrowserRendererService;
import com.habitat.api.service.MandateService;
import com.habitat.api.service.TokenBlocklistService;
import com.habitat.api.storage.StoredResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for the property-scoped {@link MandateController}. Mirrors
 * the slicing in {@link MandatesControllerTest}: service + security
 * collaborators mocked, filters off, every endpoint exercised at the
 * HTTP layer (status, routing, headers, body marshalling) without a
 * Spring Security or JPA context.
 */
@WebMvcTest(controllers = MandateController.class)
@AutoConfigureMockMvc(addFilters = false)
class MandateControllerTest {

    @Autowired MockMvc mvc;
    @MockBean MandateService mandates;
    @MockBean BrowserRendererService browserRenderer;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID MANDATE_ID = UUID.fromString("ffffffff-eeee-dddd-cccc-bbbbbbbbbbbb");
    private static final UUID PROP_ID    = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID AGENT_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LANDLORD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OWNER_USER  = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String MANDATE_URL = ApiRoutes.PROPERTIES + "/" + PROP_ID + "/mandate";

    // ── GET /properties/{id}/mandate ──────────────────────────────────────

    @Test
    void get_returns_current_mandate() throws Exception {
        when(mandates.getForProperty(PROP_ID)).thenReturn(Optional.of(sampleResponse()));

        mvc.perform(get(MANDATE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MANDATE_ID.toString()))
                .andExpect(jsonPath("$.propertyId").value(PROP_ID.toString()))
                .andExpect(jsonPath("$.mandateType").value("FULL_MANAGEMENT"))
                .andExpect(jsonPath("$.status").value("PENDING_LANDLORD_APPROVAL"))
                .andExpect(jsonPath("$.feePercent").value(8.0))
                .andExpect(jsonPath("$.landlord.userId").value(OWNER_USER.toString()));
    }

    @Test
    void get_returns_404_when_no_mandate_issued() throws Exception {
        when(mandates.getForProperty(PROP_ID)).thenReturn(Optional.empty());

        mvc.perform(get(MANDATE_URL))
                .andExpect(status().isNotFound());
    }

    // ── PUT /properties/{id}/mandate ──────────────────────────────────────

    @Test
    void issue_creates_mandate_and_returns_201() throws Exception {
        when(mandates.issue(eq(PROP_ID), any())).thenReturn(sampleResponse());

        mvc.perform(put(MANDATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mandateType":"FULL_MANAGEMENT","feePercent":8.0,
                                 "landlordEmail":"thandi@example.co.za"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MANDATE_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_LANDLORD_APPROVAL"));

        verify(mandates).issue(eq(PROP_ID), any());
    }

    @Test
    void issue_rejects_body_with_no_mandate_type() throws Exception {
        // @NotNull mandateType — bean validation rejects with the app's
        // 422 VALIDATION_FAILED contract before the service is touched.
        mvc.perform(put(MANDATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feePercent":8.0,"landlordEmail":"thandi@example.co.za"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("mandateType"));

        verify(mandates, never()).issue(any(), any());
    }

    // ── GET /properties/{id}/mandate/history ──────────────────────────────

    @Test
    void history_returns_event_timeline() throws Exception {
        HistoryEventResponse issued = new HistoryEventResponse(
                HistoryEventKind.ISSUED,
                OffsetDateTime.parse("2026-05-28T13:26:27Z"),
                AGENT_ID, "Naledi M.",
                Map.of("mandateType", "FULL_MANAGEMENT", "feePercent", "8.0"));
        HistoryEventResponse approved = new HistoryEventResponse(
                HistoryEventKind.APPROVED,
                OffsetDateTime.parse("2026-05-28T15:00:00Z"),
                OWNER_USER, "Thandi Mokoena",
                Map.of("signedName", "Thandi Mokoena"));
        when(mandates.getHistory(PROP_ID))
                .thenReturn(new MandateHistoryResponse(List.of(issued, approved)));

        mvc.perform(get(MANDATE_URL + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[0].kind").value("ISSUED"))
                .andExpect(jsonPath("$.events[0].byUserName").value("Naledi M."))
                .andExpect(jsonPath("$.events[0].payload.mandateType").value("FULL_MANAGEMENT"))
                .andExpect(jsonPath("$.events[1].kind").value("APPROVED"))
                .andExpect(jsonPath("$.events[1].payload.signedName").value("Thandi Mokoena"));
    }

    // ── GET /properties/{id}/mandate/pdf ──────────────────────────────────

    @Test
    void download_pdf_renders_and_forwards_auth_header() throws Exception {
        byte[] pdf = "%PDF-1.7 fake".getBytes(StandardCharsets.UTF_8);
        when(browserRenderer.renderUrlToPdf("/print/mandate/" + PROP_ID, "Bearer token-xyz"))
                .thenReturn(pdf);

        mvc.perform(get(MANDATE_URL + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-xyz"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"mandate-" + PROP_ID + ".pdf\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().bytes(pdf));

        // Read access is checked before any expensive render kicks off.
        verify(mandates).requirePdfReadable(PROP_ID);
        verify(browserRenderer).renderUrlToPdf("/print/mandate/" + PROP_ID, "Bearer token-xyz");
    }

    // ── GET /properties/{id}/mandate/signed ───────────────────────────────

    @Test
    void download_signed_streams_the_stored_pdf() throws Exception {
        byte[] signed = "%PDF-1.7 signed".getBytes(StandardCharsets.UTF_8);
        when(mandates.openSignedPdf(PROP_ID)).thenReturn(
                new StoredResource(new ByteArrayInputStream(signed), MediaType.APPLICATION_PDF_VALUE, signed.length));

        // StreamingResponseBody is dispatched async — start, then dispatch.
        MvcResult started = mvc.perform(get(MANDATE_URL + "/signed"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(signed.length)))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"mandate-signed-" + PROP_ID + ".pdf\""))
                .andExpect(content().bytes(signed));
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
