package com.habitat.api.event;

import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.entity.Property;
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

/**
 * Cancellation routes to whichever party DIDN'T cancel: if the tenant
 * pulled out, the manager gets the push; if the manager pulled out,
 * the tenant gets it. The actor sees a toast from the API response —
 * no need to push to themselves.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewingCancelledListener {

    private final ViewingRepository viewings;
    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onViewingCancelled(ViewingCancelledEvent event) {
        Viewing viewing = viewings.findById(event.viewingId()).orElse(null);
        if (viewing == null) return;

        var unit = viewing.getUnit();
        Property property = unit == null ? null : unit.getProperty();
        User tenant = viewing.getTenant();
        User manager = property == null ? null : property.getManager();
        UUID cancelledBy = viewing.getCancelledBy();
        if (cancelledBy == null || tenant == null || manager == null) return;

        boolean tenantCancelled = cancelledBy.equals(tenant.getId());
        User recipient = tenantCancelled ? manager : tenant;
        // CTA depends on recipient — manager goes to /viewings, tenant
        // to /my-viewings.
        String actionUrl = tenantCancelled ? "/viewings" : "/my-viewings";

        String propertyTitle = property == null || property.getTitle() == null
                ? "the property" : property.getTitle();
        String date = ViewingSlot.formatDate(viewing.getScheduledAt());
        String time = ViewingSlot.formatTime(viewing.getScheduledAt());

        tryPush(recipient,
                NotificationType.VIEWING_CANCELLED,
                NotificationMessages.VIEWING_CANCELLED_TITLE,
                TemplateUtils.format(
                        NotificationMessages.VIEWING_CANCELLED_BODY,
                        TemplatePlaceholders.P_PROPERTY_TITLE, propertyTitle,
                        TemplatePlaceholders.P_VIEWING_DATE, date,
                        TemplatePlaceholders.P_VIEWING_TIME, time),
                actionUrl,
                NotificationMessages.VIEWING_CANCELLED_ACTION_LABEL,
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
}
