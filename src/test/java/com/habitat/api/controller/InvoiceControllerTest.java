package com.habitat.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.invoice.InvoiceResponse;
import com.habitat.api.dto.invoice.PayInvoiceRequest;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.InvoiceStatus;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.JwtService;
import com.habitat.api.security.SecurityUtils;
import com.habitat.api.service.InvoiceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean InvoiceService invoices;
    @MockBean SecurityUtils security;
    @MockBean JwtService jwtService;
    @MockBean TokenBlocklistService blocklist;
    @MockBean JwtAuthenticationFilter jwtFilter;

    private static final UUID INVOICE_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID APP_ID     = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID TENANT_ID  = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID UNIT_ID    = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final UUID PROP_ID    = UUID.fromString("33333333-4444-5555-6666-777777777777");

    @Test
    void listMine_returns_caller_invoices() throws Exception {
        when(invoices.listForTenant()).thenReturn(List.of(stub(InvoiceStatus.PENDING)));

        mvc.perform(get(ApiRoutes.INVOICES + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(INVOICE_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].invoiceRef").value("HB-INV-ABCDEF"));
        verify(invoices).listForTenant();
    }

    @Test
    void get_returns_single_invoice() throws Exception {
        when(invoices.getById(INVOICE_ID)).thenReturn(stub(InvoiceStatus.PENDING));

        mvc.perform(get(ApiRoutes.INVOICES + "/" + INVOICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INVOICE_ID.toString()));
        verify(invoices).getById(INVOICE_ID);
    }

    @Test
    void pay_flips_status_and_returns_payload() throws Exception {
        when(invoices.pay(eq(INVOICE_ID), any())).thenReturn(stub(InvoiceStatus.PAID));

        mvc.perform(post(ApiRoutes.INVOICES + "/" + INVOICE_ID + "/pay").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PayInvoiceRequest("OZOW-X"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        verify(invoices).pay(eq(INVOICE_ID), any());
    }

    @Test
    void pay_accepts_an_empty_body() throws Exception {
        when(invoices.pay(eq(INVOICE_ID), any())).thenReturn(stub(InvoiceStatus.PAID));

        mvc.perform(post(ApiRoutes.INVOICES + "/" + INVOICE_ID + "/pay").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private static InvoiceResponse stub(InvoiceStatus status) {
        return new InvoiceResponse(
                INVOICE_ID,
                "HB-INV-ABCDEF",
                APP_ID,
                TENANT_ID,
                status,
                new BigDecimal("5000"),
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                OffsetDateTime.parse("2026-05-14T10:00:00Z"),
                OffsetDateTime.parse("2026-05-21T10:00:00Z"),
                status == InvoiceStatus.PAID ? OffsetDateTime.parse("2026-05-14T11:00:00Z") : null,
                status == InvoiceStatus.PAID ? "OZOW-X" : null,
                OffsetDateTime.parse("2026-05-14T10:00:00Z"),
                new InvoiceResponse.ApplicationRef(APP_ID,
                        status == InvoiceStatus.PAID ? ApplicationStatus.DEPOSIT_PAID : ApplicationStatus.INVOICE_SENT),
                new InvoiceResponse.UnitRef(UNIT_ID, "Unit 1", "1",
                        new BigDecimal("5000"), 1, 1, null),
                new InvoiceResponse.PropertyRef(PROP_ID, "Olive Court", "Sandton", "Joburg", "GP"),
                new InvoiceResponse.Snapshots("Tenant Name", "Olive Court", "Sandton, Joburg")
        );
    }
}
