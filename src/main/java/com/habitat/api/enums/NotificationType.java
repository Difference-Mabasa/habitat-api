package com.habitat.api.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * The specific event a notification represents. Each type knows its
 * {@link NotificationCategory} (the preference bucket) and the set of
 * channels it's eligible for by default; the resolver narrows that set
 * via the recipient's {@link com.habitat.api.entity.NotificationPreference}
 * rows at push time.
 *
 * Stored as STRING. Adding a new type is a source-only change. To split
 * a type across channels, add a new value and route differently — never
 * encode channel routing on the caller.
 */
public enum NotificationType {

    /**
     * Post-registration welcome. Delivered both in-app (the bell
     * drawer's "Complete Profile" nudge) and via email for users who
     * leave the tab. Users can mute the email side by toggling
     * ONBOARDING × EMAIL off in their preferences.
     */
    WELCOME(NotificationCategory.ONBOARDING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Tenant-side confirmation when both lease signatures land. Pushed
     * from {@code LeaseSignedListener} via the {@link
     * com.habitat.api.event.LeaseSignedEvent} AFTER_COMMIT hook. Routed
     * under MESSAGING because it's an application-response style event —
     * the tenant is hearing back about the application they submitted.
     */
    MOVE_IN_CONFIRMED_TENANT(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Landlord-side mirror: the lease has been signed by both parties
     * and move-in is locked in.
     */
    LEASE_SIGNED_LANDLORD(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL));

    private final NotificationCategory category;
    private final Set<NotificationChannel> defaultChannels;

    NotificationType(NotificationCategory category, Set<NotificationChannel> defaultChannels) {
        this.category = category;
        this.defaultChannels = Set.copyOf(defaultChannels);
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public Set<NotificationChannel> getDefaultChannels() {
        return defaultChannels;
    }
}
