package com.habitat.api.enums;

/**
 * Lifecycle of a deposit invoice. Issued by the platform when a
 * landlord approves an application; tenant pays it (mocked for now);
 * landlord can void it if the application's later withdrawn.
 */
public enum InvoiceStatus {
    PENDING,
    PAID,
    VOIDED,
    EXPIRED
}
