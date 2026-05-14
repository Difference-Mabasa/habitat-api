package com.habitat.api.event;

import com.habitat.api.entity.Invoice;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.repository.InvoiceRepository;
import com.habitat.api.service.LeaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to {@link InvoicePaidEvent} by generating the lease and
 * advancing the application toward {@code LEASE_PENDING_SIGNATURES}.
 *
 * <p>Closes TECH_DEBT ARCH-03 for the payment-to-lease step:
 * InvoiceService no longer holds a reference to LeaseService.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InvoicePaidListener {

    private final InvoiceRepository invoices;
    private final LeaseService leases;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInvoicePaid(InvoicePaidEvent event) {
        Invoice invoice = invoices.findById(event.invoiceId()).orElse(null);
        if (invoice == null) {
            log.warn("invoice {} vanished before lease generation — skipping",
                    event.invoiceId());
            return;
        }
        var application = invoice.getApplication();
        if (application == null) {
            log.debug("invoice {} has no application (archived) — skipping lease generation",
                    invoice.getInvoiceRef());
            return;
        }
        if (application.getStatus() != ApplicationStatus.DEPOSIT_PAID) {
            log.debug("application {} no longer at DEPOSIT_PAID ({}) — skipping lease generation",
                    application.getId(), application.getStatus());
            return;
        }
        leases.issueForPaidApplication(application);
        log.info("invoice {} paid → lease generated for application {}",
                invoice.getInvoiceRef(), application.getId());
    }
}
