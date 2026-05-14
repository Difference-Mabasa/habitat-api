package com.habitat.api.constants;

/**
 * User-facing notification copy. Title + body templates keyed by
 * notification kind. {@code {placeholder}} tokens are substituted at
 * push time via {@link com.habitat.api.util.TemplateUtils#format}.
 *
 * Per the API's development-standards "no hardcoded strings" rule, every
 * notification title/body lives here — never inlined into a service.
 * Translators get a single file to operate on when localisation lands.
 */
public final class NotificationMessages {

    // ── WELCOME ─────────────────────────────────────────────────────────
    public static final String WELCOME_TITLE = "Welcome to Habitat!";

    /**
     * Body for the welcome notification. Sent the moment a new user is
     * persisted, with a CTA into the in-profile onboarding flow.
     */
    public static final String WELCOME_BODY =
            "Hi {firstName}! Your account is ready. Complete your profile — add a bio, "
                    + "verify your identity, and tell us what you're looking for so landlords can "
                    + "match you with the right spot.";

    public static final String WELCOME_ACTION_LABEL = "Complete Profile";

    // ── MOVE-IN (both lease signatures landed) ─────────────────────────
    public static final String MOVE_IN_TENANT_TITLE =
            "Welcome to your new home!";
    public static final String MOVE_IN_TENANT_BODY =
            "The lease for {propertyTitle} is signed by both parties. "
                    + "Your move-in date is {startDate} — view the next steps "
                    + "on the move-in checklist.";
    public static final String MOVE_IN_TENANT_ACTION_LABEL = "View move-in";

    public static final String LEASE_SIGNED_LANDLORD_TITLE =
            "Lease signed by both parties";
    public static final String LEASE_SIGNED_LANDLORD_BODY =
            "{tenantName} has signed the lease for {propertyTitle}. "
                    + "Move-in is confirmed for {startDate}.";
    public static final String LEASE_SIGNED_LANDLORD_ACTION_LABEL = "View lease";

    // ── APPLICATION SUBMITTED ───────────────────────────────────────────
    public static final String APPLICATION_RECEIVED_TITLE =
            "New application received";
    public static final String APPLICATION_RECEIVED_BODY =
            "{tenantName} applied for {unitTitle} at {propertyTitle}. "
                    + "Review the application to take action.";
    public static final String APPLICATION_RECEIVED_ACTION_LABEL = "Review application";

    public static final String APPLICATION_SUBMITTED_TENANT_TITLE =
            "Application sent";
    public static final String APPLICATION_SUBMITTED_TENANT_BODY =
            "Your application for {unitTitle} is with {landlordName}. "
                    + "Landlords typically respond within 48 hours — you'll see "
                    + "status updates on My applications.";
    public static final String APPLICATION_SUBMITTED_TENANT_ACTION_LABEL =
            "Track applications";

    private NotificationMessages() {}
}
