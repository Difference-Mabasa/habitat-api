package com.habitat.api.dto.invoice;

import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /invoices/{id}/pay}. Payment integration is
 * mocked today — the request carries an optional gateway reference so
 * callers can simulate Ozow / Stripe / EFT receipts. Real payment
 * lands in a later slice (PAY-01 / PAY-02).
 */
public record PayInvoiceRequest(
        @Size(max = 255) String paymentReference
) {}
