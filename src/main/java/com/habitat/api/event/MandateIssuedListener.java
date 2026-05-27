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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Fans out the issuance notification, branching on the resolved
 * landlord type:
 *
 * <ul>
 *   <li>ONLINE → push MANDATE_PENDING_LANDLORD_APPROVAL to the
 *       landlord-User. Their /my-mandates inbox shows the approve /
 *       reject buttons.</li>
 *   <li>OFFLINE → push MANDATE_PENDING_OFFLINE_SIGNATURE to the
 *       agent themselves — the landlord has no account, so the
 *       agent gets the reminder to email/hand them the signable
 *       PDF.</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MandateIssuedListener {

    private final MandateRepository mandates;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMandateIssued(MandateIssuedEvent event) {
        Mandate mandate = mandates.findById(event.mandateId()).orElse(null);
        if (mandate == null) {
            log.warn("mandate {} vanished before issue notifications — skipping",
                    event.mandateId());
            return;
        }
        Property property = mandate.getProperty();
        Landlord landlord = property == null ? null : property.getLandlord();
        if (landlord == null) {
            log.warn("mandate {} has no resolved landlord on its property — skipping",
                    mandate.getId());
            return;
        }

        String propertyTitle = property.getTitle() == null ? "your property" : property.getTitle();
        String agentName = displayName(mandate.getAgent());
        String landlordName = landlord.displayName();
        String feePercent = formatFee(mandate.getFeePercent());
        String mandateType = labelFor(mandate.getMandateType());

        if (landlord.getType() == LandlordType.ONLINE && landlord.getUser() != null) {
            tryPush(landlord.getUser(),
                    NotificationType.MANDATE_PENDING_LANDLORD_APPROVAL,
                    NotificationMessages.MANDATE_PENDING_LANDLORD_APPROVAL_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.MANDATE_PENDING_LANDLORD_APPROVAL_BODY,
                            TemplatePlaceholders.P_AGENT_NAME, agentName,
                            TemplatePlaceholders.P_MANDATE_TYPE, mandateType,
                            TemplatePlaceholders.P_FEE_PERCENT, feePercent,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                    "/my-mandates",
                    NotificationMessages.MANDATE_PENDING_LANDLORD_APPROVAL_ACTION_LABEL,
                    mandate.getId());
        } else if (mandate.getAgent() != null) {
            // OFFLINE landlord — push the reminder to the agent so
            // they know the PDF is ready to email/hand over.
            tryPush(mandate.getAgent(),
                    NotificationType.MANDATE_PENDING_OFFLINE_SIGNATURE,
                    NotificationMessages.MANDATE_PENDING_OFFLINE_SIGNATURE_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.MANDATE_PENDING_OFFLINE_SIGNATURE_BODY,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                            TemplatePlaceholders.P_LANDLORD_NAME, landlordName),
                    "/property/" + property.getId() + "?ctx=agent",
                    NotificationMessages.MANDATE_PENDING_OFFLINE_SIGNATURE_ACTION_LABEL,
                    mandate.getId());
        }
        log.info("mandate {} issued — notifications fanned out (landlord type={})",
                mandate.getId(), landlord.getType());
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

    private static String formatFee(BigDecimal fee) {
        if (fee == null) return "—";
        return fee.stripTrailingZeros().toPlainString();
    }

    private static String labelFor(com.habitat.api.enums.MandateType type) {
        if (type == null) return "mandate";
        return switch (type) {
            case FULL_MANAGEMENT -> "full management";
            case TENANT_FIND -> "tenant find";
            case LETTING_AND_INSPECTIONS -> "letting & inspections";
        };
    }
}
