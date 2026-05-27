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
 * Tenant-side notification when the landlord parks the application
 * at ON_HOLD. Mirrors {@link ApplicationRejectedListener}'s shape —
 * the decision note (which is how the reviewer typically says
 * "send me a fresher payslip") is folded into the body.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationOnHoldListener {

    private final ApplicationRepository applications;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onApplicationOnHold(ApplicationOnHoldEvent event) {
        Application application = applications.findById(event.applicationId()).orElse(null);
        if (application == null) {
            log.warn("application {} vanished before on-hold notifications — skipping",
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
        String note = decisionSentence(application.getDecisionNote());

        tryPush(tenant,
                NotificationType.APPLICATION_ON_HOLD,
                NotificationMessages.APPLICATION_ON_HOLD_TITLE,
                TemplateUtils.format(
                        NotificationMessages.APPLICATION_ON_HOLD_BODY,
                        TemplatePlaceholders.P_LANDLORD_NAME, landlordName,
                        TemplatePlaceholders.P_UNIT_TITLE, unitTitle,
                        TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                        TemplatePlaceholders.P_DECISION_NOTE, note),
                "/my-apps",
                NotificationMessages.APPLICATION_ON_HOLD_ACTION_LABEL,
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

    private static String decisionSentence(String note) {
        if (note == null || note.isBlank()) return "";
        String trimmed = note.trim();
        char last = trimmed.charAt(trimmed.length() - 1);
        boolean punctuated = last == '.' || last == '!' || last == '?';
        return "Their note: \"" + trimmed + (punctuated ? "\"" : ".\"");
    }
}
