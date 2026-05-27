package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Mandate;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.MandateStatus;
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
 * Terminal-state acknowledgement once the mandate is ACTIVE. Fires
 * for both paths into ACTIVE:
 *
 * <ul>
 *   <li>Online landlord approved
 *       ({@link com.habitat.api.service.MandateService#approveByLandlord})</li>
 *   <li>Offline signed PDF uploaded + agent attested
 *       ({@link com.habitat.api.service.MandateService#uploadSigned})</li>
 * </ul>
 *
 * <p>Pushes to the agent always; pushes to the landlord-User only
 * when ONLINE (offline owners have no Habitat account to push to).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MandateActiveListener {

    private final MandateRepository mandates;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMandateActive(MandateActiveEvent event) {
        Mandate mandate = mandates.findById(event.mandateId()).orElse(null);
        if (mandate == null) {
            log.warn("mandate {} vanished before active notifications — skipping",
                    event.mandateId());
            return;
        }
        if (mandate.getStatus() != MandateStatus.ACTIVE) {
            log.debug("mandate {} no longer ACTIVE ({}) — skipping active notification",
                    mandate.getId(), mandate.getStatus());
            return;
        }
        Property property = mandate.getProperty();
        Landlord landlord = property == null ? null : property.getLandlord();
        String propertyTitle = property == null || property.getTitle() == null
                ? "your property" : property.getTitle();
        String agentName = displayName(mandate.getAgent());
        String agentPropertyHref = property == null
                ? null : "/property/" + property.getId() + "?ctx=agent";
        String landlordPropertyHref = property == null
                ? null : "/property/" + property.getId() + "?ctx=landlord";

        User agent = mandate.getAgent();
        if (agent != null) {
            tryPush(agent,
                    NotificationType.MANDATE_ACTIVE,
                    NotificationMessages.MANDATE_ACTIVE_AGENT_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.MANDATE_ACTIVE_AGENT_BODY,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                    agentPropertyHref,
                    NotificationMessages.MANDATE_ACTIVE_AGENT_ACTION_LABEL,
                    mandate.getId());
        }

        if (landlord != null
                && landlord.getType() == LandlordType.ONLINE
                && landlord.getUser() != null
                && (agent == null || !landlord.getUser().getId().equals(agent.getId()))) {
            tryPush(landlord.getUser(),
                    NotificationType.MANDATE_ACTIVE,
                    NotificationMessages.MANDATE_ACTIVE_LANDLORD_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.MANDATE_ACTIVE_LANDLORD_BODY,
                            TemplatePlaceholders.P_AGENT_NAME, agentName,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                    landlordPropertyHref,
                    NotificationMessages.MANDATE_ACTIVE_LANDLORD_ACTION_LABEL,
                    mandate.getId());
        }
        log.info("mandate {} ACTIVE — notifications fanned out", mandate.getId());
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
        if (u == null) return "your agent";
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }
}
