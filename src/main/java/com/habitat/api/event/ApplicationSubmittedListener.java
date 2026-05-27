package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.repository.ApplicationRepository;
import com.habitat.api.service.NotificationService;
import com.habitat.api.util.TemplateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Fans out notifications when an application is submitted.
 *
 * <ul>
 *   <li><b>Landlord / agent</b> — {@link NotificationType#APPLICATION_RECEIVED}
 *       on the property's manager (backroom-aligned).</li>
 *   <li><b>Landlord</b> — same notification on the property's registered
 *       landlord too, if landlord.id ≠ manager.id (the agent-managed
 *       case). Keeps the landlord in the loop even when an agent handles
 *       the listing.</li>
 *   <li><b>Tenant</b> — {@link NotificationType#APPLICATION_SUBMITTED_TENANT}
 *       confirmation. Habitat extension; backroom didn't notify the
 *       applicant.</li>
 * </ul>
 *
 * <p>Runs AFTER_COMMIT + REQUIRES_NEW so a failure here doesn't taint
 * the apply transaction. Mirrors the {@link LeaseSignedListener} shape.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationSubmittedListener {

    private final ApplicationRepository applications;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        Application application = applications.findById(event.applicationId()).orElse(null);
        if (application == null) {
            log.warn("application {} vanished before submit notifications — skipping",
                    event.applicationId());
            return;
        }

        var unit = application.getUnit();
        var property = unit.getProperty();
        var tenant = application.getTenant();
        var manager = property.getManager();
        Landlord landlord = property.getLandlord();
        // Notify the owner-User only when the landlord has a Habitat
        // account. Offline landlords can't receive in-app pushes — the
        // manager (agent) sees the notification and relays out-of-band.
        User landlordUser = landlord != null && landlord.getType() == LandlordType.ONLINE
                ? landlord.getUser()
                : null;

        String tenantName = displayName(tenant);
        String landlordName = landlord == null ? displayName(manager) : landlord.displayName();
        String unitTitle = unit.getTitle() == null ? "the unit" : unit.getTitle();
        String propertyTitle = property.getTitle() == null ? "the property" : property.getTitle();

        // Manager (always present per V9's NOT NULL).
        if (manager != null) {
            tryPush(manager, NotificationType.APPLICATION_RECEIVED,
                    NotificationMessages.APPLICATION_RECEIVED_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.APPLICATION_RECEIVED_BODY,
                            TemplatePlaceholders.P_TENANT_NAME, tenantName,
                            TemplatePlaceholders.P_UNIT_TITLE, unitTitle,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                    "/applicant?id=" + application.getId(),
                    NotificationMessages.APPLICATION_RECEIVED_ACTION_LABEL,
                    application.getId());
        }

        // Landlord too, if they have a Habitat account and aren't the
        // same User as the manager (agent-managed case with an online
        // landlord). Offline landlords are notified via the manager.
        if (landlordUser != null
                && (manager == null || !managerEquals(manager.getId(), landlordUser.getId()))) {
            tryPush(landlordUser, NotificationType.APPLICATION_RECEIVED,
                    NotificationMessages.APPLICATION_RECEIVED_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.APPLICATION_RECEIVED_BODY,
                            TemplatePlaceholders.P_TENANT_NAME, tenantName,
                            TemplatePlaceholders.P_UNIT_TITLE, unitTitle,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                    "/applicant?id=" + application.getId(),
                    NotificationMessages.APPLICATION_RECEIVED_ACTION_LABEL,
                    application.getId());
        }

        // Tenant confirmation.
        tryPush(tenant, NotificationType.APPLICATION_SUBMITTED_TENANT,
                NotificationMessages.APPLICATION_SUBMITTED_TENANT_TITLE,
                TemplateUtils.format(
                        NotificationMessages.APPLICATION_SUBMITTED_TENANT_BODY,
                        TemplatePlaceholders.P_UNIT_TITLE, unitTitle,
                        TemplatePlaceholders.P_LANDLORD_NAME, landlordName),
                "/my-apps",
                NotificationMessages.APPLICATION_SUBMITTED_TENANT_ACTION_LABEL,
                application.getId());

        log.info("application {} submitted — notifications fanned out", application.getId());
    }

    private void tryPush(User recipient, NotificationType type, String title, String body,
                         String actionUrl, String actionLabel, UUID refId) {
        try {
            notifications.push(recipient, type, title, body, actionUrl, actionLabel, refId);
        } catch (RuntimeException ex) {
            log.warn("notification push failed for {} ({}): {}",
                    recipient.getId(), type, ex.getMessage(), ex);
        }
    }

    private static boolean managerEquals(UUID a, UUID b) {
        return a != null && a.equals(b);
    }

    private static String displayName(User u) {
        if (u == null) return "the user";
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }
}
