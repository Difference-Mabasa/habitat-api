package com.habitat.api.service.statemachine;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.entity.Application;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.exception.ConflictException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for legal {@link ApplicationStatus} transitions.
 *
 * <p>Before this helper existed, the state machine was implicit and
 * spread across four services (ApplicationService, InvoiceService,
 * LeaseService, LeaseSignedListener). Adding a new transition meant
 * trusting the author to remember every other site that touches the
 * column. The compiler couldn't help.
 *
 * <p>Every status write goes through {@link #transition} — illegal
 * transitions throw {@link ConflictException} which the global handler
 * maps to 409. Closes TECH_DEBT ARCH-02.
 */
public final class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> LEGAL = Map.ofEntries(
            Map.entry(ApplicationStatus.SUBMITTED,
                    EnumSet.of(ApplicationStatus.AWAITING_DOCUMENTS,
                            ApplicationStatus.UNDER_REVIEW,
                            ApplicationStatus.APPROVED,
                            ApplicationStatus.REJECTED,
                            ApplicationStatus.ON_HOLD,
                            ApplicationStatus.WITHDRAWN,
                            ApplicationStatus.EXPIRED)),
            Map.entry(ApplicationStatus.AWAITING_DOCUMENTS,
                    EnumSet.of(ApplicationStatus.DOCUMENTS_SUBMITTED,
                            ApplicationStatus.REJECTED,
                            ApplicationStatus.WITHDRAWN,
                            ApplicationStatus.EXPIRED)),
            Map.entry(ApplicationStatus.DOCUMENTS_SUBMITTED,
                    EnumSet.of(ApplicationStatus.UNDER_REVIEW,
                            ApplicationStatus.APPROVED,
                            ApplicationStatus.REJECTED,
                            ApplicationStatus.ON_HOLD,
                            ApplicationStatus.WITHDRAWN,
                            ApplicationStatus.EXPIRED)),
            Map.entry(ApplicationStatus.UNDER_REVIEW,
                    EnumSet.of(ApplicationStatus.APPROVED,
                            ApplicationStatus.REJECTED,
                            ApplicationStatus.ON_HOLD,
                            ApplicationStatus.WITHDRAWN,
                            ApplicationStatus.EXPIRED)),
            Map.entry(ApplicationStatus.ON_HOLD,
                    EnumSet.of(ApplicationStatus.UNDER_REVIEW,
                            ApplicationStatus.APPROVED,
                            ApplicationStatus.REJECTED,
                            ApplicationStatus.WITHDRAWN,
                            ApplicationStatus.EXPIRED)),
            Map.entry(ApplicationStatus.APPROVED,
                    EnumSet.of(ApplicationStatus.INVOICE_SENT,
                            ApplicationStatus.REJECTED,
                            ApplicationStatus.WITHDRAWN)),
            Map.entry(ApplicationStatus.INVOICE_SENT,
                    EnumSet.of(ApplicationStatus.DEPOSIT_PAID,
                            ApplicationStatus.EXPIRED,
                            ApplicationStatus.WITHDRAWN)),
            Map.entry(ApplicationStatus.DEPOSIT_PAID,
                    EnumSet.of(ApplicationStatus.LEASE_GENERATED,
                            ApplicationStatus.LEASE_PENDING_SIGNATURES,
                            ApplicationStatus.WITHDRAWN)),
            Map.entry(ApplicationStatus.LEASE_GENERATED,
                    EnumSet.of(ApplicationStatus.LEASE_PENDING_SIGNATURES,
                            ApplicationStatus.WITHDRAWN)),
            Map.entry(ApplicationStatus.LEASE_PENDING_SIGNATURES,
                    EnumSet.of(ApplicationStatus.COMPLETED,
                            ApplicationStatus.WITHDRAWN)),
            // Terminal — no outgoing transitions.
            Map.entry(ApplicationStatus.COMPLETED, EnumSet.noneOf(ApplicationStatus.class)),
            Map.entry(ApplicationStatus.REJECTED,  EnumSet.noneOf(ApplicationStatus.class)),
            Map.entry(ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class)),
            Map.entry(ApplicationStatus.EXPIRED,   EnumSet.noneOf(ApplicationStatus.class))
    );

    private ApplicationStateMachine() {}

    /** True if {@code current → next} is in the legal-transition matrix. */
    public static boolean canTransition(ApplicationStatus current, ApplicationStatus next) {
        Set<ApplicationStatus> allowed = LEGAL.get(current);
        return allowed != null && allowed.contains(next);
    }

    /**
     * Apply {@code next} to {@code application}. No-op if the application
     * is already at {@code next} (idempotent). Throws
     * {@link ConflictException} on an illegal source-to-target combo.
     */
    public static void transition(Application application, ApplicationStatus next) {
        ApplicationStatus current = application.getStatus();
        if (current == next) return;
        if (!canTransition(current, next)) {
            throw new ConflictException(ErrorMessages.APPLICATION_INVALID_TRANSITION);
        }
        application.setStatus(next);
    }
}
