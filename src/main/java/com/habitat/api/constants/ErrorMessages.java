package com.habitat.api.constants;

/**
 * Every user-facing error message. Adding a literal in a service or controller
 * is blocked by the pre-commit hook — put it here instead.
 */
public final class ErrorMessages {

    // Auth
    public static final String INVALID_CREDENTIALS = "Email or password is incorrect.";
    public static final String EMAIL_ALREADY_REGISTERED = "An account with this email already exists.";
    public static final String EMAIL_NOT_VERIFIED = "Verify your email before signing in.";
    public static final String JWT_INVALID = "Your session token is invalid or expired.";
    public static final String JWT_SECRET_TOO_SHORT = "JWT secret must be at least 32 bytes.";
    public static final String ROLE_NOT_SELF_ASSIGNABLE = "That role cannot be self-assigned on registration.";
    public static final String ROLE_NOT_OWNED = "You don't have that role on your account.";
    public static final String AUTH_REQUIRED = "Authentication required.";
    public static final String REFRESH_TOKEN_REVOKED = "Your session has been revoked. Sign in again.";

    // Resources
    public static final String USER_NOT_FOUND = "User not found.";
    public static final String NOTIFICATION_NOT_FOUND = "Notification not found.";
    public static final String PROPERTY_NOT_FOUND = "Property not found.";
    public static final String PROPERTY_NEEDS_UNIT_BEFORE_PUBLISH =
            "Add at least one unit before publishing this listing.";
    public static final String MANDATE_FEE_REQUIRED =
            "Agent-managed listings must include a mandate fee.";
    public static final String MANDATE_FEE_DISALLOWED =
            "Landlord-direct listings can't carry a mandate fee.";
    public static final String UNKNOWN_AMENITY =
            "One or more selected amenities aren't in the catalogue.";
    public static final String MANDATE_NOT_FOUND = "Mandate not found.";
    public static final String MANDATE_REQUIRES_AGENT_MODE =
            "Mandates only apply to agent-managed listings.";
    public static final String MANDATE_NOT_READY_FOR_UPLOAD =
            "A signed mandate can only be uploaded after issue.";
    public static final String MANDATE_NOT_READY_FOR_LANDLORD_DECISION =
            "This mandate isn't awaiting your approval right now.";
    public static final String VIEWING_NOT_FOUND = "Viewing not found.";
    public static final String VIEWING_INVALID_TRANSITION =
            "That viewing can't move to that state right now.";
    public static final String VIEWING_SCHEDULED_AT_PAST =
            "Viewings have to be scheduled in the future.";
    public static final String VIEWING_UNIT_NOT_AVAILABLE =
            "That unit isn't accepting viewings right now.";
    public static final String MANDATE_PDF_NOT_READY =
            "The mandate PDF hasn't been generated yet.";
    public static final String MANDATE_SIGNED_NOT_AVAILABLE =
            "No signed mandate has been uploaded yet.";
    public static final String LANDLORD_ID_NUMBER_INVALID =
            "That SA ID number doesn't pass the Luhn check. Double-check the 13 digits.";
    public static final String LANDLORD_NOT_FOUND = "Landlord not found.";
    public static final String LANDLORD_EDIT_FORBIDDEN =
            "Only the agent who first captured this landlord can change their details. Request an update via the inbox.";
    public static final String LANDLORD_REQUIRED_FIELDS =
            "Offline landlord capture needs at least a first name, last name, email, and ID number.";
    public static final String UNIT_NOT_FOUND = "Unit not found.";
    public static final String UNIT_NOT_AVAILABLE = "This unit isn't accepting applications right now.";
    public static final String APPLICATION_ALREADY_SUBMITTED = "You've already applied for this unit.";
    public static final String APPLICATION_NOT_FOUND = "Application not found.";
    public static final String APPLICATION_NOT_REVIEWABLE =
            "This application can't be reviewed in its current state.";
    public static final String APPLICATION_INVALID_TRANSITION =
            "Illegal application status change.";
    public static final String DOCS_UPLOAD_WRONG_STATUS =
            "Documents can only be uploaded while the application is awaiting them.";
    public static final String INVOICE_NOT_FOUND = "Invoice not found.";
    public static final String INVOICE_NOT_PAYABLE =
            "This invoice can't be paid in its current state.";
    public static final String LEASE_NOT_FOUND = "Lease not found.";
    public static final String LEASE_NOT_SIGNABLE =
            "This lease can't be signed in its current state.";
    public static final String LEASE_ALREADY_SIGNED =
            "You've already signed this lease.";
    public static final String LEASE_OTP_INVALID =
            "That code is incorrect or has expired. Request a new one.";
    public static final String LEASE_PDF_NOT_READY =
            "The signed PDF is generated once both parties have signed.";
    public static final String SYSTEM_IN_APP_CANNOT_BE_MUTED =
            "Account & security in-app alerts can't be muted.";

    // Storage
    public static final String INVALID_FILE_PATH = "Invalid file path.";
    public static final String FILE_TYPE_NOT_ALLOWED = "That file type is not allowed.";
    public static final String FILE_TOO_LARGE = "File exceeds the maximum upload size.";

    // Common
    public static final String FORBIDDEN = "You don't have permission to do that.";
    public static final String INTERNAL = "An unexpected error occurred.";
    public static final String SERVICE_UNAVAILABLE = "A dependency is temporarily unavailable.";

    private ErrorMessages() {}
}
