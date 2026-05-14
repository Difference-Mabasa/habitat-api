package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.invoice.InvoiceResponse;
import com.habitat.api.dto.invoice.PayInvoiceRequest;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.Invoice;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.InvoiceStatus;
import com.habitat.api.exception.ConflictException;
import com.habitat.api.exception.ForbiddenException;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.InvoiceRepository;
import com.habitat.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Deposit-invoice lifecycle. Created when a landlord approves an
 * application; paid (mocked) by the tenant. Paying advances the parent
 * application to {@code DEPOSIT_PAID}, which unblocks lease generation
 * in the next slice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final SecurityUtils security;

    /** Default validity window for an unpaid invoice — 7 days from issue. */
    private static final long INVOICE_TTL_DAYS = 7L;

    /**
     * Idempotently issue an invoice for a freshly-approved application.
     * No-ops (returns the existing invoice) if one was already issued
     * for this application, so a flapping APPROVE → ON_HOLD → APPROVE
     * doesn't duplicate billing.
     */
    @Transactional
    public Invoice issueForApprovedApplication(Application application) {
        return invoices.findByApplication_Id(application.getId())
                .orElseGet(() -> {
                    var unit = application.getUnit();
                    var property = unit.getProperty();
                    BigDecimal price = unit.getPrice();
                    BigDecimal deposit = price == null ? BigDecimal.ZERO : price;
                    BigDecimal firstMonth = price == null ? BigDecimal.ZERO : price;
                    BigDecimal total = deposit.add(firstMonth);
                    var tenant = application.getTenant();
                    Invoice fresh = Invoice.builder()
                            .application(application)
                            .tenant(tenant)
                            .landlord(property.getManager())
                            .unit(unit)
                            .property(property)
                            .depositAmount(deposit)
                            .firstMonthRent(firstMonth)
                            .totalAmount(total)
                            .status(InvoiceStatus.PENDING)
                            .invoiceRef(nextInvoiceRef())
                            .issuedAt(OffsetDateTime.now())
                            .expiresAt(OffsetDateTime.now().plusDays(INVOICE_TTL_DAYS))
                            // BUG-02: snapshot the displayable identity
                            // so old invoices stay accurate after edits.
                            .tenantNameSnapshot(displayName(tenant))
                            .propertyTitleSnapshot(property.getTitle())
                            .propertyAddressSnapshot(formatAddress(property))
                            .build();
                    Invoice saved = invoices.save(fresh);
                    log.info("invoice {} issued for application {} (total={})",
                            saved.getInvoiceRef(), application.getId(), total);
                    return saved;
                });
    }

    /** Caller's own invoices, newest first — powers the pay-deposit screen. */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> listForTenant() {
        UUID me = security.requireUserId();
        return invoices.findByTenant_IdOrderByCreatedAtDesc(me).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    /** Single invoice — visible to the tenant who owns it. */
    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        UUID me = security.requireUserId();
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.INVOICE_NOT_FOUND));
        if (!me.equals(invoice.getTenant().getId())) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }
        return InvoiceResponse.from(invoice);
    }

    /**
     * Mock-pay an invoice. Marks the invoice PAID and advances the
     * parent application from {@code INVOICE_SENT} to
     * {@code DEPOSIT_PAID}. Idempotent: re-paying a PAID invoice is a
     * no-op returning the same payload.
     */
    @Transactional
    public InvoiceResponse pay(UUID id, PayInvoiceRequest req) {
        UUID me = security.requireUserId();
        Invoice invoice = invoices.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.INVOICE_NOT_FOUND));
        if (!me.equals(invoice.getTenant().getId())) {
            throw new ForbiddenException(ErrorMessages.FORBIDDEN);
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return InvoiceResponse.from(invoice);
        }
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException(ErrorMessages.INVOICE_NOT_PAYABLE);
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(OffsetDateTime.now());
        invoice.setPaymentReference(req == null ? null : req.paymentReference());

        // The application trace pointer is nullable after V22 — if the
        // application has been archived since the invoice was issued
        // we still record the payment, we just can't advance any
        // downstream state. That's expected for late-paid invoices.
        Application application = invoice.getApplication();
        if (application != null
                && application.getStatus() == ApplicationStatus.INVOICE_SENT) {
            com.habitat.api.service.statemachine.ApplicationStateMachine
                    .transition(application, ApplicationStatus.DEPOSIT_PAID);
        }

        // AFTER_COMMIT event drives lease generation. Decouples
        // InvoiceService from LeaseService (TECH_DEBT ARCH-03).
        events.publishEvent(new com.habitat.api.event.InvoicePaidEvent(invoice.getId()));

        log.info("invoice {} paid by {} → application status = {}",
                invoice.getInvoiceRef(), me,
                application == null ? "(archived)" : application.getStatus());
        return InvoiceResponse.from(invoice);
    }

    /** Mints a short, human-readable invoice reference (HB-INV-XXXXXX). */
    private static String nextInvoiceRef() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "HB-INV-" + suffix;
    }

    private static String displayName(com.habitat.api.entity.User u) {
        if (u == null) return null;
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }

    private static String formatAddress(com.habitat.api.entity.Property p) {
        if (p == null) return null;
        return java.util.stream.Stream.of(
                        p.getAddressLine(), p.getSuburb(), p.getCity(), p.getPostalCode())
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }
}
