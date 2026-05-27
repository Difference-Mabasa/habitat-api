package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.DevSpecimensService;
import com.habitat.api.service.TokenBlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DevSpecimensController.class)
@AutoConfigureMockMvc(addFilters = false)
class DevSpecimensControllerTest {

    private static final byte[] FAKE_PDF = "%PDF-1.4 specimen".getBytes();

    @Autowired MockMvc mvc;
    @MockBean DevSpecimensService specimens;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    @Test
    void mandate_specimen_streams_pdf_bytes_inline() throws Exception {
        when(specimens.renderMandateSpecimen()).thenReturn(FAKE_PDF);

        mvc.perform(get(ApiRoutes.DEV_SPECIMEN_MANDATE_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"mandate-specimen.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(FAKE_PDF));

        verify(specimens).renderMandateSpecimen();
    }

    @Test
    void lease_specimen_streams_pdf_bytes_inline() throws Exception {
        when(specimens.renderLeaseSpecimen()).thenReturn(FAKE_PDF);

        mvc.perform(get(ApiRoutes.DEV_SPECIMEN_LEASE_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"lease-specimen.pdf\""))
                .andExpect(content().bytes(FAKE_PDF));

        verify(specimens).renderLeaseSpecimen();
    }

    @Test
    void invoice_specimen_streams_pdf_bytes_inline() throws Exception {
        when(specimens.renderInvoiceSpecimen()).thenReturn(FAKE_PDF);

        mvc.perform(get(ApiRoutes.DEV_SPECIMEN_INVOICE_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"invoice-specimen.pdf\""))
                .andExpect(content().bytes(FAKE_PDF));

        verify(specimens).renderInvoiceSpecimen();
    }
}
