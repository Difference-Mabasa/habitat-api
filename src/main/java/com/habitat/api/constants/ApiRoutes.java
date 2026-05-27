package com.habitat.api.constants;

/**
 * Canonical URL paths for every endpoint. Referenced from controllers
 * (@RequestMapping) and SecurityConfig (path matchers). Never hardcode a
 * literal path string in either location.
 */
public final class ApiRoutes {

    public static final String BASE = "/api/v1";

    // Auth
    public static final String AUTH = BASE + "/auth";
    public static final String AUTH_REGISTER = AUTH + "/register";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_REFRESH = AUTH + "/refresh";
    public static final String AUTH_LOGOUT = AUTH + "/logout";
    public static final String AUTH_OAUTH2_EXCHANGE = AUTH + "/oauth2/exchange";

    // Health / docs
    public static final String HEALTH = BASE + "/health";
    public static final String DOCS = "/swagger-ui/**";
    public static final String DOCS_API = "/v3/api-docs/**";

    // Users
    public static final String USERS = BASE + "/users";
    public static final String USERS_ME = USERS + "/me";
    public static final String USERS_ME_ACTIVE_ROLE = USERS_ME + "/active-role";
    public static final String USERS_ME_BANK_ACCOUNT = USERS_ME + "/bank-account";

    // Notifications
    public static final String NOTIFICATIONS = BASE + "/notifications";
    public static final String NOTIFICATIONS_UNREAD_COUNT = NOTIFICATIONS + "/unread-count";
    public static final String NOTIFICATIONS_READ_ALL = NOTIFICATIONS + "/read-all";

    // Per-user preferences
    public static final String PREFERENCES = BASE + "/preferences";
    public static final String PREFERENCES_NOTIFICATIONS = PREFERENCES + "/notifications";

    // Properties + units
    public static final String PROPERTIES = BASE + "/properties";
    public static final String PROPERTIES_POPULAR_AREAS = PROPERTIES + "/popular-areas";
    public static final String PROPERTIES_TOP_RATED = PROPERTIES + "/top-rated";
    public static final String PROPERTIES_OWNED_BY_ME = PROPERTIES + "/owned-by-me";
    public static final String UNITS = BASE + "/units";

    // Landlords — distinct identity from User. See entity Landlord.java.
    public static final String LANDLORDS = BASE + "/landlords";
    public static final String LANDLORDS_LOOKUP = LANDLORDS + "/lookup";

    // Applications
    public static final String APPLICATIONS = BASE + "/applications";

    // Amenities (public catalogue)
    public static final String AMENITIES = BASE + "/amenities";

    // Mandates are scoped to a property — paths constructed at the
    // controller as `/properties/{id}/mandate/...`. The cross-property
    // inbox lives at the top-level `/mandates` namespace.
    public static final String MANDATES = BASE + "/mandates";
    public static final String MANDATES_AWAITING_MY_APPROVAL = MANDATES + "/awaiting-my-approval";

    // Invoices
    public static final String INVOICES = BASE + "/invoices";

    // Leases
    public static final String LEASES = BASE + "/leases";

    // Files (ownership-checked download paths — Phase 7)
    public static final String FILES = BASE + "/files";
    public static final String FILES_IMAGES = FILES + "/images";

    // Image lifecycle (the GET is on FILES_IMAGES; DELETE is on the row)
    public static final String PROPERTY_IMAGES = BASE + "/property-images";
    public static final String UNIT_IMAGES = BASE + "/unit-images";

    // Landing-page aggregates (public)
    public static final String LANDING = BASE + "/landing";
    public static final String LANDING_STATS = LANDING + "/stats";
    public static final String LANDING_CITIES = LANDING + "/cities";

    // Dev hub — non-prod specimen PDFs the dev gallery embeds.
    public static final String DEV_SPECIMENS = BASE + "/dev/specimens";
    public static final String DEV_SPECIMEN_MANDATE_PDF = DEV_SPECIMENS + "/mandate.pdf";
    public static final String DEV_SPECIMEN_LEASE_PDF = DEV_SPECIMENS + "/lease.pdf";
    public static final String DEV_SPECIMEN_INVOICE_PDF = DEV_SPECIMENS + "/invoice.pdf";

    /**
     * UI route the welcome notification's CTA points at. Not an API path —
     * shipped here so any future surface that needs to deep-link to the
     * user's editable profile reads it from one place.
     *
     * Previously pointed at {@code /profile/onboarding} (a now-removed
     * wizard); /profile is the single edit surface that hydrates from
     * {@code GET /users/me} and autosaves per field group on blur.
     */
    public static final String UI_PROFILE = "/profile";

    private ApiRoutes() {}
}
