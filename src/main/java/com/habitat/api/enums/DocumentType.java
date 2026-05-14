package com.habitat.api.enums;

/**
 * Categories of supporting documents a landlord may require from a
 * tenant applicant. Mirrors backroom-api's {@code DocumentType} enum
 * (minus the very-rare ones — credit consent, landlord reference, and
 * "other" land if a future ask surfaces).
 *
 * <p>Stored as STRING.
 */
public enum DocumentType {
    SA_ID,
    PASSPORT,
    PAYSLIPS_3_MONTHS,
    BANK_STATEMENTS_3_MONTHS,
    EMPLOYMENT_LETTER,
    PROOF_OF_ADDRESS
}
