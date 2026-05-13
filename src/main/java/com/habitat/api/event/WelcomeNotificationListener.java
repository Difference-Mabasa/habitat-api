package com.habitat.api.event;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.constants.NotificationMessages;
import com.habitat.api.constants.TemplatePlaceholders;
import com.habitat.api.enums.NotificationType;
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
 * Creates the in-app welcome notification when a user registers.
 *
 * Listens AFTER_COMMIT so a notification only lands for users that have
 * actually been persisted; if AuthService.register rolls back, no
 * notification leaks. The handler runs in its own transaction
 * (Propagation.REQUIRES_NEW) so its failure doesn't affect callers in
 * tests or future synchronous chained listeners.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WelcomeNotificationListener {

    private final NotificationService notifications;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            String body = TemplateUtils.format(
                    NotificationMessages.WELCOME_BODY,
                    TemplatePlaceholders.P_FIRST_NAME, event.user().getFirstName());

            notifications.push(
                    event.user(),
                    NotificationType.WELCOME,
                    NotificationMessages.WELCOME_TITLE,
                    body,
                    ApiRoutes.UI_PROFILE,
                    NotificationMessages.WELCOME_ACTION_LABEL,
                    null);
        } catch (RuntimeException ex) {
            // Welcome notifications are best-effort. A failure here must not
            // tarnish a successful registration — the user is already in.
            // The next admin sweep can backfill missing rows from the audit
            // log if it ever matters.
            log.warn("welcome notification failed for user {}: {}",
                    event.userId(), ex.getMessage(), ex);
        }
    }
}
