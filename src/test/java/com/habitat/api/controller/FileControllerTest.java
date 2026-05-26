package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.FileService;
import com.habitat.api.service.FileService.DownloadHandle;
import com.habitat.api.service.TokenBlocklistService;
import com.habitat.api.storage.StoredResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired MockMvc mvc;
    @MockBean FileService files;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID APP_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DOC_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void download_streams_the_file_with_response_headers_set() throws Exception {
        byte[] payload = "doc-bytes".getBytes();
        when(files.openApplicationDocument(APP_ID, DOC_ID))
                .thenReturn(new DownloadHandle(
                        new StoredResource(new ByteArrayInputStream(payload),
                                "application/pdf", payload.length),
                        "id-front.pdf"));

        mvc.perform(get(ApiRoutes.FILES + "/documents/" + APP_ID + "/" + DOC_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Length", String.valueOf(payload.length)))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"id-front.pdf\""))
                .andExpect(content().bytes(payload));
    }

    @Test
    void download_falls_back_to_octet_stream_when_mime_is_null() throws Exception {
        byte[] payload = new byte[]{1, 2, 3};
        when(files.openApplicationDocument(APP_ID, DOC_ID))
                .thenReturn(new DownloadHandle(
                        new StoredResource(new ByteArrayInputStream(payload),
                                null, payload.length),
                        "mystery.bin"));

        mvc.perform(get(ApiRoutes.FILES + "/documents/" + APP_ID + "/" + DOC_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"));
    }

    @Test
    void filename_with_a_quote_in_it_is_sanitized_for_the_header() throws Exception {
        byte[] payload = new byte[]{1};
        when(files.openApplicationDocument(APP_ID, DOC_ID))
                .thenReturn(new DownloadHandle(
                        new StoredResource(new ByteArrayInputStream(payload),
                                "application/pdf", 1L),
                        "weird\"name.pdf"));

        mvc.perform(get(ApiRoutes.FILES + "/documents/" + APP_ID + "/" + DOC_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"weird_name.pdf\""));
    }

    @Test
    void forbidden_from_service_maps_to_403() throws Exception {
        when(files.openApplicationDocument(APP_ID, DOC_ID))
                .thenThrow(new ForbiddenException("nope"));

        mvc.perform(get(ApiRoutes.FILES + "/documents/" + APP_ID + "/" + DOC_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void not_found_from_service_maps_to_404() throws Exception {
        when(files.openApplicationDocument(APP_ID, DOC_ID))
                .thenThrow(new ResourceNotFoundException("missing"));

        mvc.perform(get(ApiRoutes.FILES + "/documents/" + APP_ID + "/" + DOC_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
