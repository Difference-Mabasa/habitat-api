package com.habitat.api.event;

import java.util.UUID;

/**
 * Published by {@link com.habitat.api.service.ApplicationService#review}
 * when a landlord approves an application.
 *
 * <p>An AFTER_COMMIT listener handles the downstream side effect —
 * issuing the deposit invoice and bumping the application to
 * {@code INVOICE_SENT}. Decoupling the chain so ApplicationService
 * doesn't depend on InvoiceService (TECH_DEBT.md ARCH-03).
 */
public record ApplicationApprovedEvent(UUID applicationId) {}
