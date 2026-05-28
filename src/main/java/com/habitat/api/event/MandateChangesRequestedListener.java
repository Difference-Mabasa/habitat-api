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
 * Notifies the agent when the online landlord opens a structured
 * change request. The agent's resolution panel on
 * {@code /my-mandates/:propertyId} renders the items + comment for
 * one-click apply, revise-in-wizard, or withdraw.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MandateChangesRequestedListener {

    private final MandateRepository mandates;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMandateChangesRequested(MandateChangesRequestedEvent event) {
        Mandate mandate = mandates.findById(event.mandateId()).orElse(null);
        if (mandate == null) {
            log.warn("mandate {} vanished before changes-requested notifications — skipping",
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
                NotificationType.MANDATE_CHANGES_REQUESTED,
                NotificationMessages.MANDATE_CHANGES_REQUESTED_TITLE,
                TemplateUtils.format(
                        NotificationMessages.MANDATE_CHANGES_REQUESTED_BODY,
                        TemplatePlaceholders.P_LANDLORD_NAME, landlordName,
                        TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                property == null ? null : "/my-mandates/" + property.getId(),
                NotificationMessages.MANDATE_CHANGES_REQUESTED_ACTION_LABEL,
                mandate.getId());
        log.info("mandate {} changes-requested — agent {} notified",
                mandate.getId(), agent.getId());
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
