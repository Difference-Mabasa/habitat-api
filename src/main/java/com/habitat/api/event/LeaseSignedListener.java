package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Lease;
import com.habitat.api.enums.ApplicationStatus;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.enums.UnitStatus;
import com.habitat.api.repository.LeaseRepository;
import com.habitat.api.service.NotificationService;
import com.habitat.api.util.TemplateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Move-in completion side effects. Fires after a {@link LeaseSignedEvent}
 * commits — i.e. once both parties have actually signed.
 *
 * <p>Responsibilities (slice 4):
 * <ul>
 *   <li>Application status → {@code COMPLETED}.</li>
 *   <li>Unit status → {@code OCCUPIED}.</li>
 *   <li>Push tenant + landlord notifications.</li>
 * </ul>
 *
 * <p>Runs in {@code REQUIRES_NEW} so its failure doesn't poison the
 * sign transaction in tests or future chained listeners. Mirrors the
 * shape of {@link WelcomeNotificationListener}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LeaseSignedListener {

    private static final DateTimeFormatter PRETTY_DATE =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final LeaseRepository leases;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLeaseSigned(LeaseSignedEvent event) {
        Lease lease = leases.findById(event.leaseId()).orElse(null);
        if (lease == null) {
            log.warn("lease {} vanished before move-in side effects ran — skipping",
                    event.leaseId());
            return;
        }

        // Lifecycle state flips — application + unit.
        var application = lease.getApplication();
        if (application != null && application.getStatus() != ApplicationStatus.COMPLETED) {
            application.setStatus(ApplicationStatus.COMPLETED);
        }
        var unit = lease.getUnit();
        if (unit != null && unit.getStatus() != UnitStatus.OCCUPIED) {
            unit.setStatus(UnitStatus.OCCUPIED);
        }
        log.info("move-in completed: lease {} → application COMPLETED, unit OCCUPIED",
                lease.getLeaseRef());

        // Party notifications — best-effort, mirrors the welcome listener.
        try {
            pushNotifications(lease);
        } catch (RuntimeException ex) {
            log.warn("move-in notifications failed for lease {}: {}",
                    lease.getLeaseRef(), ex.getMessage(), ex);
        }
    }

    private void pushNotifications(Lease lease) {
        var tenant = lease.getTenant();
        var landlord = lease.getLandlord();
        String propertyTitle = lease.getProperty().getTitle();
        String startDate = lease.getStartDate() == null
                ? "the agreed date"
                : PRETTY_DATE.format(lease.getStartDate());
        String tenantName = tenant.getFirstName() == null
                ? "Your tenant"
                : tenant.getFirstName() + (tenant.getSurname() == null ? "" : " " + tenant.getSurname());

        String tenantBody = TemplateUtils.format(
                NotificationMessages.MOVE_IN_TENANT_BODY,
                TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                TemplatePlaceholders.P_START_DATE, startDate);
        notifications.push(
                tenant,
                NotificationType.MOVE_IN_CONFIRMED_TENANT,
                NotificationMessages.MOVE_IN_TENANT_TITLE,
                tenantBody,
                "/move-in",
                NotificationMessages.MOVE_IN_TENANT_ACTION_LABEL,
                lease.getId());

        if (landlord != null) {
            String landlordBody = TemplateUtils.format(
                    NotificationMessages.LEASE_SIGNED_LANDLORD_BODY,
                    TemplatePlaceholders.P_TENANT_NAME, tenantName,
                    TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                    TemplatePlaceholders.P_START_DATE, startDate);
            notifications.push(
                    landlord,
                    NotificationType.LEASE_SIGNED_LANDLORD,
                    NotificationMessages.LEASE_SIGNED_LANDLORD_TITLE,
                    landlordBody,
                    "/lease?id=" + lease.getId(),
                    NotificationMessages.LEASE_SIGNED_LANDLORD_ACTION_LABEL,
                    lease.getId());
        }
    }
}
