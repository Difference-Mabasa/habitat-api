package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Application;
import com.habitat.api.entity.User;
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
 * Sends the tenant-side "your application was approved" push. Sits
 * alongside {@link ApplicationApprovedListener} (invoice issuance) —
 * separated so the notification side effect doesn't tangle with the
 * billing side effect. Both listeners react to
 * {@link ApplicationApprovedEvent}; ordering doesn't matter because
 * the notification doesn't reference the invoice (the body just says
 * "the deposit invoice will land in your inbox shortly").
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationApprovedNotificationListener {

    private final ApplicationRepository applications;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationApproved(ApplicationApprovedEvent event) {
        Application application = applications.findById(event.applicationId()).orElse(null);
        if (application == null) {
            log.warn("application {} vanished before approval notifications — skipping",
                    event.applicationId());
            return;
        }
        User tenant = application.getTenant();
        if (tenant == null) return;

        var unit = application.getUnit();
        var property = unit == null ? null : unit.getProperty();
        var manager = property == null ? null : property.getManager();
        String unitTitle = unit == null || unit.getTitle() == null ? "the unit" : unit.getTitle();
        String propertyTitle = property == null || property.getTitle() == null
                ? "the property" : property.getTitle();
        String landlordName = displayName(manager);

        tryPush(tenant,
                NotificationType.APPLICATION_APPROVED,
                NotificationMessages.APPLICATION_APPROVED_TITLE,
                TemplateUtils.format(
                        NotificationMessages.APPLICATION_APPROVED_BODY,
                        TemplatePlaceholders.P_LANDLORD_NAME, landlordName,
                        TemplatePlaceholders.P_UNIT_TITLE, unitTitle,
                        TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                "/my-apps",
                NotificationMessages.APPLICATION_APPROVED_ACTION_LABEL,
                application.getId());
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

    private static String displayName(User u) {
        if (u == null) return "the landlord";
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }
}
