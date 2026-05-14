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
    public static final String UNIT_NOT_FOUND = "Unit not found.";
    public static final String UNIT_NOT_AVAILABLE = "This unit isn't accepting applications right now.";
    public static final String APPLICATION_ALREADY_SUBMITTED = "You've already applied for this unit.";
    public static final String APPLICATION_NOT_FOUND = "Application not found.";
    public static final String APPLICATION_NOT_REVIEWABLE =
            "This application can't be reviewed in its current state.";
    public static final String DOCS_UPLOAD_WRONG_STATUS =
            "Documents can only be uploaded while the application is awaiting them.";
    public static final String INVOICE_NOT_FOUND = "Invoice not found.";
    public static final String INVOICE_NOT_PAYABLE =
            "This invoice can't be paid in its current state.";
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
