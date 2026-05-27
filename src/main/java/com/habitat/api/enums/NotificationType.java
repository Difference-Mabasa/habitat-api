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
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Landlord / agent gets this when a tenant submits an application
     * against one of their units. Backroom-aligned naming.
     */
    APPLICATION_RECEIVED(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Tenant-side mirror: "we got your application" confirmation. Habitat
     * extension over backroom — backroom only pushed to the manager.
     */
    APPLICATION_SUBMITTED_TENANT(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    // ── Property listing lifecycle ────────────────────────────────────
    // Pushed from the property + mandate event listeners. Manager
    // always gets manager-targeted notifications; owner-targeted
    // pushes only fire when landlord.user is non-null (ONLINE).

    /**
     * Property has just transitioned DRAFT|UNLISTED → LISTED. Sent to
     * the manager (acknowledging publish) and to the owner-User when
     * different (agent-managed case with an online landlord).
     */
    PROPERTY_PUBLISHED(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Online landlord — the agent has just issued a mandate against
     * one of their properties. They tap Approve/Reject in their
     * /my-mandates inbox to flip the mandate to ACTIVE or REJECTED.
     */
    MANDATE_PENDING_LANDLORD_APPROVAL(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Agent-side reminder for the offline-landlord flow: the mandate
     * PDF is ready to email / hand to the landlord for signature.
     * Landlord has no Habitat account to push to.
     */
    MANDATE_PENDING_OFFLINE_SIGNATURE(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Sent to the agent when the online landlord approves the
     * mandate. Mirrors backroom's MANDATE_APPROVED.
     */
    MANDATE_APPROVED(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Sent to the agent when the online landlord rejects the mandate.
     */
    MANDATE_REJECTED(NotificationCategory.MESSAGING,
            EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),

    /**
     * Final-state acknowledgement when the mandate transitions to
     * ACTIVE — either the online landlord approved or the offline
     * flow completed (signed upload + agent attestation). Pushed to
     * the agent always; pushed to the landlord-User when ONLINE.
     */
    MANDATE_ACTIVE(NotificationCategory.MESSAGING,
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
