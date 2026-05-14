package com.habitat.api.event;

import com.habitat.api.entity.Application;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.repository.ApplicationRepository;
import com.habitat.api.service.InvoiceService;
import com.habitat.api.service.statemachine.ApplicationStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to {@link ApplicationApprovedEvent} by issuing the deposit
 * invoice and advancing the application to {@code INVOICE_SENT}.
 *
 * <p>Runs AFTER_COMMIT + REQUIRES_NEW so the side effect only fires if
 * the approval actually committed; a failure here doesn't poison the
 * landlord-review transaction. Mirrors {@link LeaseSignedListener}.
 *
 * <p>This is the boundary that closes TECH_DEBT ARCH-03 for the
 * approval-to-invoice step: ApplicationService no longer holds a
 * reference to InvoiceService.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationApprovedListener {

    private final ApplicationRepository applications;
    private final InvoiceService invoices;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationApproved(ApplicationApprovedEvent event) {
        Application application = applications.findById(event.applicationId()).orElse(null);
        if (application == null) {
            log.warn("application {} vanished before invoice issuance — skipping",
                    event.applicationId());
            return;
        }
        if (application.getStatus() != ApplicationStatus.APPROVED) {
            log.debug("application {} no longer at APPROVED ({}) — skipping invoice issuance",
                    application.getId(), application.getStatus());
            return;
        }
        invoices.issueForApprovedApplication(application);
        ApplicationStateMachine.transition(application, ApplicationStatus.INVOICE_SENT);
        log.info("application {} → INVOICE_SENT (invoice issued)", application.getId());
    }
}
