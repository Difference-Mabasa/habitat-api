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

    // ── PROPERTY PUBLISHED ──────────────────────────────────────────────
    /** Manager-facing — "your listing is live". */
    public static final String PROPERTY_PUBLISHED_MANAGER_TITLE =
            "Your listing is live";
    public static final String PROPERTY_PUBLISHED_MANAGER_BODY =
            "{propertyTitle} is now visible on Habitat. Tenants browsing "
                    + "{propertyAddress} can see it and apply. View the public "
                    + "listing to check how it presents.";
    public static final String PROPERTY_PUBLISHED_MANAGER_ACTION_LABEL =
            "View listing";

    /** Owner-facing variant — only fires when listing was agent-managed
     *  and the owner has a Habitat account distinct from the manager. */
    public static final String PROPERTY_PUBLISHED_OWNER_TITLE =
            "Your property is live on Habitat";
    public static final String PROPERTY_PUBLISHED_OWNER_BODY =
            "{agentName} has published {propertyTitle} on your behalf. "
                    + "Tenants can now browse and apply — the listing is in your "
                    + "Properties workspace.";
    public static final String PROPERTY_PUBLISHED_OWNER_ACTION_LABEL =
            "Open property";

    // ── MANDATE — PENDING LANDLORD APPROVAL (online flow) ───────────────
    public static final String MANDATE_PENDING_LANDLORD_APPROVAL_TITLE =
            "Mandate awaiting your approval";
    public static final String MANDATE_PENDING_LANDLORD_APPROVAL_BODY =
            "{agentName} has issued a {mandateType} mandate at {feePercent}% "
                    + "for {propertyTitle}. Review the terms and approve to publish.";
    public static final String MANDATE_PENDING_LANDLORD_APPROVAL_ACTION_LABEL =
            "Review mandate";

    // ── MANDATE — PENDING OFFLINE SIGNATURE (agent reminder) ────────────
    public static final String MANDATE_PENDING_OFFLINE_SIGNATURE_TITLE =
            "Mandate PDF ready to send";
    public static final String MANDATE_PENDING_OFFLINE_SIGNATURE_BODY =
            "We've generated the signable PDF for {propertyTitle}. Email it "
                    + "to {landlordName} or download to send via your own channel; "
                    + "once they've signed, upload the signed copy back here.";
    public static final String MANDATE_PENDING_OFFLINE_SIGNATURE_ACTION_LABEL =
            "Open mandate";

    // ── MANDATE — APPROVED (by landlord) ────────────────────────────────
    public static final String MANDATE_APPROVED_TITLE =
            "Mandate approved";
    public static final String MANDATE_APPROVED_BODY =
            "{landlordName} approved the mandate for {propertyTitle}. "
                    + "You can publish the listing whenever the rest of the "
                    + "publish checklist is clear.";
    public static final String MANDATE_APPROVED_ACTION_LABEL =
            "Open property";

    // ── MANDATE — REJECTED (by landlord) ────────────────────────────────
    public static final String MANDATE_REJECTED_TITLE =
            "Mandate rejected";
    public static final String MANDATE_REJECTED_BODY =
            "{landlordName} rejected the mandate for {propertyTitle}. "
                    + "Reach out to revise the terms or fee — the listing stays "
                    + "in DRAFT until a fresh mandate is approved.";
    public static final String MANDATE_REJECTED_ACTION_LABEL =
            "Open property";

    // ── MANDATE — ACTIVE (terminal state) ───────────────────────────────
    /** Agent-facing — the offline mandate cycle (signed + attested) or
     *  online approval has completed and the mandate is now ACTIVE. */
    public static final String MANDATE_ACTIVE_AGENT_TITLE =
            "Mandate is active";
    public static final String MANDATE_ACTIVE_AGENT_BODY =
            "The mandate for {propertyTitle} is now ACTIVE. The publish "
                    + "checklist will clear the mandate gate; everything else "
                    + "ready and the listing can go live.";
    public static final String MANDATE_ACTIVE_AGENT_ACTION_LABEL =
            "Open property";

    /** Landlord-facing — only fires for ONLINE owners (offline owners
     *  can't receive an in-app push). */
    public static final String MANDATE_ACTIVE_LANDLORD_TITLE =
            "Mandate now active for your property";
    public static final String MANDATE_ACTIVE_LANDLORD_BODY =
            "Your mandate with {agentName} for {propertyTitle} is active. "
                    + "The listing can publish at any time and rent will be "
                    + "collected into Habitat's trust account on your behalf.";
    public static final String MANDATE_ACTIVE_LANDLORD_ACTION_LABEL =
            "Open property";

    private NotificationMessages() {}
}
