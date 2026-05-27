package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.User;
import com.habitat.api.entity.Viewing;
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

/** Tenant-side confirmation that the slot is locked in. */
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewingApprovedListener {

    private final ViewingRepository viewings;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onViewingApproved(ViewingApprovedEvent event) {
        Viewing viewing = viewings.findById(event.viewingId()).orElse(null);
        if (viewing == null) return;
        User tenant = viewing.getTenant();
        if (tenant == null) return;

        var unit = viewing.getUnit();
        var property = unit == null ? null : unit.getProperty();
        var manager = property == null ? null : property.getManager();
        String propertyTitle = property == null || property.getTitle() == null
                ? "the property" : property.getTitle();
        String landlordName = displayName(manager);
        String date = ViewingSlot.formatDate(viewing.getScheduledAt());
        String time = ViewingSlot.formatTime(viewing.getScheduledAt());

        tryPush(tenant,
                NotificationType.VIEWING_APPROVED,
                NotificationMessages.VIEWING_APPROVED_TITLE,
                TemplateUtils.format(
                        NotificationMessages.VIEWING_APPROVED_BODY,
                        TemplatePlaceholders.P_LANDLORD_NAME, landlordName,
                        TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                        TemplatePlaceholders.P_VIEWING_DATE, date,
                        TemplatePlaceholders.P_VIEWING_TIME, time),
                "/my-viewings",
                NotificationMessages.VIEWING_APPROVED_ACTION_LABEL,
                viewing.getId());
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
