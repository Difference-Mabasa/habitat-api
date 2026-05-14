package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.invoice.InvoiceResponse;
import com.habitat.api.dto.invoice.PayInvoiceRequest;
import com.habitat.api.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-facing invoice endpoints. Sits on {@code /invoices}. Real
 * payment gateways land in a later slice — {@code POST /{id}/pay}
 * mocks success and advances the parent application state.
 */
@RestController
@RequestMapping(ApiRoutes.INVOICES)
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoices;

    /** Caller's own invoices, newest first. */
    @GetMapping("/me")
    public List<InvoiceResponse> listMine() {
        return invoices.listForTenant();
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return invoices.getById(id);
    }

    /** Mock-pay an invoice. Returns the updated invoice. */
    @PostMapping("/{id}/pay")
    public InvoiceResponse pay(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) PayInvoiceRequest req
    ) {
        return invoices.pay(id, req);
    }
}
