package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Landlord;
import com.habitat.api.entity.Property;
import com.habitat.api.entity.Unit;
import com.habitat.api.entity.User;
import com.habitat.api.entity.Viewing;
import com.habitat.api.enums.LandlordType;
import com.habitat.api.enums.NotificationType;
import com.habitat.api.repository.ViewingRepository;
import com.habitat.api.service.NotificationService;
import com.habitat.api.util.TemplateUtils;
import com.habitat.api.util.ViewingSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Pushes VIEWING_REQUESTED to the manager (and the online owner-User
 * if distinct). CTA goes to /viewings — the manager's calendar where
 * approve/reject lives.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewingRequestedListener {

    private final ViewingRepository viewings;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onViewingRequested(ViewingRequestedEvent event) {
        Viewing viewing = viewings.findById(event.viewingId()).orElse(null);
        if (viewing == null) {
            log.warn("viewing {} vanished before requested notifications — skipping",
                    event.viewingId());
            return;
        }

        Unit unit = viewing.getUnit();
        Property property = unit == null ? null : unit.getProperty();
        if (property == null) return;
        User manager = property.getManager();
        Landlord landlord = property.getLandlord();
        User onlineOwner = landlord != null && landlord.getType() == LandlordType.ONLINE
                ? landlord.getUser()
                : null;
        User tenant = viewing.getTenant();

        String tenantName = displayName(tenant);
        String propertyTitle = property.getTitle() == null ? "your property" : property.getTitle();
        String date = ViewingSlot.formatDate(viewing.getScheduledAt());
        String time = ViewingSlot.formatTime(viewing.getScheduledAt());

        if (manager != null) {
            tryPush(manager,
                    NotificationType.VIEWING_REQUESTED,
                    NotificationMessages.VIEWING_REQUESTED_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.VIEWING_REQUESTED_BODY,
                            TemplatePlaceholders.P_TENANT_NAME, tenantName,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                            TemplatePlaceholders.P_VIEWING_DATE, date,
                            TemplatePlaceholders.P_VIEWING_TIME, time),
                    "/viewings",
                    NotificationMessages.VIEWING_REQUESTED_ACTION_LABEL,
                    viewing.getId());
        }
        if (onlineOwner != null
                && (manager == null || !onlineOwner.getId().equals(manager.getId()))) {
            tryPush(onlineOwner,
                    NotificationType.VIEWING_REQUESTED,
                    NotificationMessages.VIEWING_REQUESTED_TITLE,
                    TemplateUtils.format(
                            NotificationMessages.VIEWING_REQUESTED_BODY,
                            TemplatePlaceholders.P_TENANT_NAME, tenantName,
                            TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                            TemplatePlaceholders.P_VIEWING_DATE, date,
                            TemplatePlaceholders.P_VIEWING_TIME, time),
                    "/viewings?ctx=landlord",
                    NotificationMessages.VIEWING_REQUESTED_ACTION_LABEL,
                    viewing.getId());
        }
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
        if (u == null) return "A tenant";
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getSurname() == null ? "" : u.getSurname();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }
}
