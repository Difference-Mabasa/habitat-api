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

    private NotificationMessages() {}
}
