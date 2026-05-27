package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.User;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.enums.PropertyStatus;
import com.habitat.api.repository.PropertyRepository;
import com.habitat.api.service.NotificationService;
import com.habitat.api.util.TemplateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fans out the publish acknowledgement once a property transitions
 * DRAFT|UNLISTED → LISTED:
 *
 * <ul>
 *   <li><b>Manager</b> — always pushed. CTA goes to the public
 *       listing so they can see how it presents.</li>
 *   <li><b>Owner (User behind Landlord)</b> — pushed only when the
 *       landlord is ONLINE and distinct from the manager (agent-
 *       managed listing where the owner has a Habitat account).</li>
 * </ul>
 *
 * <p>AFTER_COMMIT + REQUIRES_NEW so a notification failure can't
 * roll back the publish.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PropertyPublishedListener {

    private final PropertyRepository properties;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPropertyPublished(PropertyPublishedEvent event) {
        Property property = properties.findById(event.propertyId()).orElse(null);
        if (property == null) {
            log.warn("property {} vanished before publish notifications — skipping",
                    event.propertyId());
            return;
        }
        if (property.getStatus() != PropertyStatus.LISTED) {
            log.debug("property {} no longer LISTED ({}) — skipping publish notification",
                    property.getId(), property.getStatus());
            return;
        }

        User manager = property.getManager();
        Landlord landlord = property.getLandlord();
        User ownerUser = landlord != null && landlord.getType() == LandlordType.ONLINE
                ? landlord.getUser()
                : null;
        String propertyTitle = property.getTitle() == null ? "your property" : property.getTitle();
        String propertyAddress = formatAddress(property);
        String agentName = displayName(manager);

        if (manager != null) {
            tryPush(manager,
                    NotificationType.PROPERTY_PUBLISHED,
                    NotificationMessages.PROPERTY_PUBLISHED_MANAGER_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.PROPERTY_PUBLISHED_MANAGER_BODY,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                            TemplatePlaceholders.P_PROPERTY_ADDRESS, propertyAddress),
                    "/property/" + property.getId(),
                    NotificationMessages.PROPERTY_PUBLISHED_MANAGER_ACTION_LABEL,
                    property.getId());
        }

        if (ownerUser != null
                && (manager == null || !ownerUser.getId().equals(manager.getId()))) {
            tryPush(ownerUser,
                    NotificationType.PROPERTY_PUBLISHED,
                    NotificationMessages.PROPERTY_PUBLISHED_OWNER_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.PROPERTY_PUBLISHED_OWNER_BODY,
                            TemplatePlaceholders.P_AGENT_NAME, agentName,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle),
                    "/property/" + property.getId() + "?ctx=landlord",
                    NotificationMessages.PROPERTY_PUBLISHED_OWNER_ACTION_LABEL,
                    property.getId());
        }

        log.info("property {} published — notifications fanned out", property.getId());
    }

    private void tryPush(User recipient, NotificationType type, String title, String body,
                         String actionUrl, String actionLabel, java.util.UUID refId) {
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

    private static String formatAddress(Property p) {
        String suburb = p.getSuburb();
        String city = p.getCity();
        if ((suburb == null || suburb.isBlank()) && (city == null || city.isBlank())) {
            return p.getAddressLine() == null ? "the property's area" : p.getAddressLine();
        }
        if (suburb == null || suburb.isBlank()) return city;
        if (city == null || city.isBlank()) return suburb;
        return suburb + ", " + city;
    }
}
