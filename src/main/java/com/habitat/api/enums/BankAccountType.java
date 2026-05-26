package com.habitat.api.enums;

/**
 * SA bank-account types tracked on {@link com.habitat.api.entity.BankAccount}.
 * Matches the dropdown options on the wizard's payout step.
 */
public enum BankAccountType {
    CHEQUE,
    SAVINGS,
    TRANSMISSION,
    BUSINESS
}
