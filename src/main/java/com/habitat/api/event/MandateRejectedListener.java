package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
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
 * Notifies the agent that the online landlord has rejected the
 * mandate. The agent reaches out to revise terms before re-issuing;
 * no terminal acknowledgement to the landlord here (their reject
 * action is the closure on their side).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MandateRejectedListener {

    private final MandateRepository mandates;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMandateRejected(MandateRejectedEvent event) {
        Mandate mandate = mandates.findById(event.mandateId()).orElse(null);
        if (mandate == null) {
            log.warn("mandate {} vanished before reject notifications — skipping",
                    event.mandateId());
            return;
        }
        User agent = mandate.getAgent();
        if (agent == null) return;

        Property property = mandate.getProperty();
        Landlord landlord = property == null ? null : property.getLandlord();
        String propertyTitle = property == null || property.getTitle() == null
                ? "your property" : property.getTitle();
        String landlordName = landlord == null ? "the landlord" : landlord.displayName();

        tryPush(agent,
                NotificationType.MANDATE_REJECTED,
                NotificationMessages.MANDATE_REJECTED_TITLE,
                TemplateUtils.format(
                        NotificationMessages.MANDATE_REJECTED_BODY,
                        TemplatePlaceholders.P_LANDLORD_NAME, landlordName,
                        TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                property == null ? null : "/property/" + property.getId() + "?ctx=agent",
                NotificationMessages.MANDATE_REJECTED_ACTION_LABEL,
                mandate.getId());
        log.info("mandate {} rejected — agent {} notified", mandate.getId(), agent.getId());
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
