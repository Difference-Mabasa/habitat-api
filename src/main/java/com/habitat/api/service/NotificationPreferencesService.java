package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.notification.NotificationPreferenceMatrix;
import com.habitat.api.dto.notification.NotificationPreferenceUpdate;
import com.habitat.api.entity.NotificationPreference;
import com.habitat.api.entity.NotificationPreference.PreferenceKey;
import com.habitat.api.enums.NotificationCategory;
import com.habitat.api.enums.NotificationChannel;
import com.habitat.api.exception.ValidationException;
import com.habitat.api.repository.NotificationPreferenceRepository;
import com.habitat.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lookups + updates for a user's notification preferences matrix.
 *
 * Defaults are implicit: a missing row in {@code notification_preferences}
 * for a (user, category, channel) tuple is interpreted as opted-in. We
 * only write a row when the user has actually toggled the cell. Keeps
 * signup cheap (no 18-row bulk insert) and lets us add new categories
 * or channels in code without backfilling rows.
 *
 * Hard rule (independent of stored preferences):
 *   SYSTEM × IN_APP is always enabled and cannot be disabled by the
 *   user. Account, security, and legal alerts must reach the bell
 *   drawer. The PATCH endpoint rejects 422 on attempted disables.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPreferencesService {

    private final NotificationPreferenceRepository repo;
    private final SecurityUtils security;

    /**
     * The full matrix the UI renders. Every (category, channel) cell is
     * emitted exactly once with its current effective state and a
     * `locked` flag for cells the user can't change.
     */
    @Transactional(readOnly = true)
    public NotificationPreferenceMatrix getMyMatrix() {
        UUID me = security.requireUserId();
        Map<PreferenceKey, Boolean> stored = new HashMap<>();
        for (NotificationPreference p : repo.findByUserId(me)) {
            stored.put(
                    new PreferenceKey(p.getUserId(), p.getCategory(), p.getChannel()),
                    p.isEnabled());
        }

        List<NotificationPreferenceMatrix.Cell> cells = new ArrayList<>();
        for (NotificationCategory category : NotificationCategory.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                boolean locked = isLocked(category, channel);
                boolean enabled = locked
                        ? true
                        : stored.getOrDefault(new PreferenceKey(me, category, channel), Boolean.TRUE);
                cells.add(new NotificationPreferenceMatrix.Cell(category, channel, enabled, locked));
            }
        }

        return new NotificationPreferenceMatrix(
                List.of(NotificationCategory.values()),
                List.of(NotificationChannel.values()),
                cells);
    }

    /**
     * Apply one cell update. Idempotent — re-toggling to the same value
     * succeeds silently. Rejects 422 when attempting to disable a locked
     * cell (currently only SYSTEM × IN_APP).
     */
    @Transactional
    public void update(NotificationPreferenceUpdate update) {
        UUID me = security.requireUserId();
        if (isLocked(update.category(), update.channel()) && !update.enabled()) {
            throw new ValidationException(ErrorMessages.SYSTEM_IN_APP_CANNOT_BE_MUTED);
        }

        repo.findByUserIdAndCategoryAndChannel(me, update.category(), update.channel())
                .ifPresentOrElse(
                        existing -> existing.setEnabled(update.enabled()),
                        () -> repo.save(NotificationPreference.builder()
                                .userId(me)
                                .category(update.category())
                                .channel(update.channel())
                                .enabled(update.enabled())
                                .build()));
        log.info("notification preference updated: user={} category={} channel={} enabled={}",
                me, update.category(), update.channel(), update.enabled());
    }

    /**
     * Used by {@link NotificationService} to filter delivery channels at
     * push time. Internal — does not call {@code SecurityUtils} because
     * push is system-initiated, not user-initiated.
     */
    @Transactional(readOnly = true)
    public boolean isOptedIn(UUID userId, NotificationCategory category, NotificationChannel channel) {
        if (isLocked(category, channel)) return true;
        return repo.findByUserIdAndCategoryAndChannel(userId, category, channel)
                .map(NotificationPreference::isEnabled)
                .orElse(true);
    }

    /**
     * The hardcoded carve-out: the SYSTEM category is always delivered
     * IN_APP regardless of preferences. SYSTEM via EMAIL or SMS stays
     * toggleable.
     */
    private static boolean isLocked(NotificationCategory category, NotificationChannel channel) {
        return category == NotificationCategory.SYSTEM && channel == NotificationChannel.IN_APP;
    }
}
