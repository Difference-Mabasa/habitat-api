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
 * Notifies the online landlord when the agent withdraws a pending
 * mandate (terminal — no more rounds). Mirrors the resubmit listener's
 * ONLINE-only gate.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MandateWithdrawnListener {

    private final MandateRepository mandates;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMandateWithdrawn(MandateWithdrawnEvent event) {
        Mandate mandate = mandates.findById(event.mandateId()).orElse(null);
        if (mandate == null) {
            log.warn("mandate {} vanished before withdraw notifications — skipping",
                    event.mandateId());
            return;
        }
        Property property = mandate.getProperty();
        Landlord landlord = property == null ? null : property.getLandlord();
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
                NotificationType.MANDATE_WITHDRAWN,
                NotificationMessages.MANDATE_WITHDRAWN_TITLE,
                TemplateUtils.format(
                        NotificationMessages.MANDATE_WITHDRAWN_BODY,
                        TemplatePlaceholders.P_AGENT_NAME, agentName,
                        TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                "/mandate-approvals/" + property.getId(),
                NotificationMessages.MANDATE_WITHDRAWN_ACTION_LABEL,
                mandate.getId());
        log.info("mandate {} withdrawn — landlord {} notified",
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
