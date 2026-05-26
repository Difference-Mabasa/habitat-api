package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.application.ApplicationDocumentResponse;
import com.habitat.api.dto.application.ApplicationResponse;
import com.habitat.api.dto.application.CreateApplicationRequest;
import com.habitat.api.dto.application.ReviewApplicationRequest;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.DocumentType;
import com.habitat.api.enums.EmploymentStatus;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.ApplicationService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean ApplicationService applications;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID UNIT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID APP_ID  = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID TENANT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PROP_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");

    @Test
    void create_returns_201_and_payload() throws Exception {
        CreateApplicationRequest req = new CreateApplicationRequest(
                UNIT_ID, "Hello!", LocalDate.parse("2026-08-01"), EmploymentStatus.EMPLOYED);
        ApplicationResponse stub = stubResponse(ApplicationStatus.SUBMITTED, "Hello!");
        when(applications.create(any(CreateApplicationRequest.class))).thenReturn(stub);

        mvc.perform(post(ApiRoutes.APPLICATIONS).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(APP_ID.toString()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.unitId").value(UNIT_ID.toString()));
    }

    @Test
    void create_rejects_a_payload_with_no_unitId() throws Exception {
        // unitId is @NotNull on the request DTO — the validation filter
        // catches it and returns 400 before the service is consulted.
        String body = "{\"message\":\"hi\"}";
        mvc.perform(post(ApiRoutes.APPLICATIONS).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void create_passes_the_request_through_to_the_service() throws Exception {
        CreateApplicationRequest req = new CreateApplicationRequest(UNIT_ID, null, null, null);
        when(applications.create(any(CreateApplicationRequest.class)))
                .thenReturn(stubResponse(ApplicationStatus.SUBMITTED, null));

        mvc.perform(post(ApiRoutes.APPLICATIONS).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated());
        verify(applications).create(any(CreateApplicationRequest.class));
    }

    @Test
    void listMine_returns_the_callers_applications() throws Exception {
        when(applications.listForTenant())
                .thenReturn(List.of(stubResponse(ApplicationStatus.SUBMITTED, null)));

        mvc.perform(get(ApiRoutes.APPLICATIONS + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(APP_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
        verify(applications).listForTenant();
    }

    @Test
    void listInbound_returns_landlord_inbox() throws Exception {
        when(applications.listInboundForLandlord())
                .thenReturn(List.of(stubResponse(ApplicationStatus.DOCUMENTS_SUBMITTED, null)));

        mvc.perform(get(ApiRoutes.APPLICATIONS + "/inbound"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DOCUMENTS_SUBMITTED"));
        verify(applications).listInboundForLandlord();
    }

    @Test
    void get_returns_single_application() throws Exception {
        when(applications.getById(APP_ID))
                .thenReturn(stubResponse(ApplicationStatus.UNDER_REVIEW, null));

        mvc.perform(get(ApiRoutes.APPLICATIONS + "/" + APP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(APP_ID.toString()))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        verify(applications).getById(APP_ID);
    }

    @Test
    void review_passes_the_action_and_note_through() throws Exception {
        ReviewApplicationRequest req = new ReviewApplicationRequest(
                ReviewApplicationRequest.Action.APPROVE, "Looks great");
        when(applications.review(eq(APP_ID), any(ReviewApplicationRequest.class)))
                .thenReturn(stubResponse(ApplicationStatus.APPROVED, null));

        mvc.perform(patch(ApiRoutes.APPLICATIONS + "/" + APP_ID + "/review").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        verify(applications).review(eq(APP_ID), any(ReviewApplicationRequest.class));
    }

    @Test
    void review_rejects_payload_with_no_action() throws Exception {
        String body = "{\"note\":\"nope\"}";
        mvc.perform(patch(ApiRoutes.APPLICATIONS + "/" + APP_ID + "/review").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void uploadDocument_accepts_multipart_and_delegates_to_service() throws Exception {
        UUID docId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        when(applications.uploadDocument(eq(APP_ID), eq(DocumentType.SA_ID), any()))
                .thenReturn(new ApplicationDocumentResponse(
                        docId, APP_ID, DocumentType.SA_ID,
                        "id-front.jpg", "image/jpeg", 1024L,
                        "/api/v1/files/documents/" + APP_ID + "/" + docId,
                        OffsetDateTime.now(), false));

        MockMultipartFile file = new MockMultipartFile(
                "file", "id-front.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart(ApiRoutes.APPLICATIONS + "/" + APP_ID + "/documents")
                        .file(file)
                        .param("docType", "SA_ID")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.docType").value("SA_ID"))
                .andExpect(jsonPath("$.fileName").value("id-front.jpg"))
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.sizeBytes").value(1024))
                .andExpect(jsonPath("$.downloadUrl")
                        .value("/api/v1/files/documents/" + APP_ID + "/" + docId));
        verify(applications).uploadDocument(eq(APP_ID), eq(DocumentType.SA_ID), any());
    }

    @Test
    void uploadDocument_rejects_missing_docType_param() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "id.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart(ApiRoutes.APPLICATIONS + "/" + APP_ID + "/documents")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    private static ApplicationResponse stubResponse(ApplicationStatus status, String message) {
        return new ApplicationResponse(
                APP_ID, UNIT_ID, TENANT_ID, status,
                message, LocalDate.parse("2026-08-01"), EmploymentStatus.EMPLOYED,
                null, null, OffsetDateTime.now(),
                new ApplicationResponse.UnitRef(UNIT_ID, "Unit 1", "1", new BigDecimal("8500"), 1, 1, null),
                new ApplicationResponse.PropertyRef(PROP_ID, "Olive Court", "Sandton", "Joburg", "GP"),
                new ApplicationResponse.TenantRef(TENANT_ID, "T", "Tester", "t@example.co.za")
        );
    }
}
