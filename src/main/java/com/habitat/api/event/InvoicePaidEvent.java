package com.habitat.api.event;

import java.util.UUID;

/**
 * Published by {@link com.habitat.api.service.InvoiceService#pay} when
 * a deposit invoice flips PENDING → PAID.
 *
 * <p>An AFTER_COMMIT listener generates the lease and advances the
 * parent application toward {@code LEASE_PENDING_SIGNATURES}.
 * Decouples InvoiceService from LeaseService (TECH_DEBT.md ARCH-03).
 */
public record InvoicePaidEvent(UUID invoiceId) {}
