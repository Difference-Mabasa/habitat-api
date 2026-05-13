package com.habitat.api.enums;

/**
 * High-level grouping that the user controls preferences against.
 *
 * Stored as STRING on both {@code notifications} and
 * {@code notification_preferences}. Adding a category is source-only —
 * no migration on the VARCHAR(32) columns.
 *
 * The {@link #SYSTEM} category × {@link NotificationChannel#IN_APP}
 * combination is enforced un-opt-outable in service code. A user can
 * mute SYSTEM via email or SMS but never in the in-app drawer (account
 * security, legal updates, lockouts).
 */
public enum NotificationCategory {
    /** Account security, legal updates, lockouts, password resets. */
    SYSTEM,
    /** Profile and account changes (role granted, email verified). */
    ACCOUNT,
    /** First-time onboarding nudges — welcome, complete profile, verify identity. */
    ONBOARDING,
    /** Inbound DMs, community replies, application responses. */
    MESSAGING,
    /** Invoices, rent due, payout completed, refunds. */
    BILLING,
    /** Recommendations, price drops, blog digests, referral offers. */
    MARKETING
}
