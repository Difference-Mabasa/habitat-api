package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.repository.MandateRepository;
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
 * Notifies the online landlord when the agent resubmits a revised
 * mandate. The landlord's detail screen shows a diff banner over the
 * summary highlighting what the agent did vs what was requested.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MandateResubmittedListener {

    private final MandateRepository mandates;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMandateResubmitted(MandateResubmittedEvent event) {
        Mandate mandate = mandates.findById(event.mandateId()).orElse(null);
        if (mandate == null) {
            log.warn("mandate {} vanished before resubmit notifications — skipping",
                    event.mandateId());
            return;
        }
        Property property = mandate.getProperty();
        Landlord landlord = property == null ? null : property.getLandlord();
        // Offline landlords can't receive an in-app push — they'll see
        // the revised mandate on the printed PDF the agent resends.
        if (landlord == null
                || landlord.getType() != LandlordType.ONLINE
                || landlord.getUser() == null) {
            return;
        }
        User recipient = landlord.getUser();
        User agent = mandate.getAgent();
        String agentName = agent == null ? "the agent" : displayName(agent);
        String propertyTitle = property.getTitle() == null
                ? "your property" : property.getTitle();

        tryPush(recipient,
                NotificationType.MANDATE_RESUBMITTED,
                NotificationMessages.MANDATE_RESUBMITTED_TITLE,
                TemplateUtils.format(
                        NotificationMessages.MANDATE_RESUBMITTED_BODY,
                        TemplatePlaceholders.P_AGENT_NAME, agentName,
                        TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                "/mandate-approvals/" + property.getId(),
                NotificationMessages.MANDATE_RESUBMITTED_ACTION_LABEL,
                mandate.getId());
        log.info("mandate {} resubmitted — landlord {} notified",
                mandate.getId(), recipient.getId());
    }

    private static String displayName(User u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
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
}
