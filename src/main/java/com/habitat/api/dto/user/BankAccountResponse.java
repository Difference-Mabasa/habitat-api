package com.habitat.api.dto.user;

import com.habitat.api.entity.BankAccount;
import com.habitat.api.enums.BankAccountType;

import java.util.UUID;

/**
 * Read shape for {@code GET /properties/{propertyId}/bank-account}.
 * Returned exactly as the wizard's payout step submitted it.
 */
public record BankAccountResponse(
        UUID id,
        String bankName,
        String accountHolder,
        String accountNumber,
        BankAccountType accountType,
        String branchCode,
        String vatNumber
) {
    public static BankAccountResponse from(BankAccount a) {
        return new BankAccountResponse(
                a.getId(),
                a.getBankName(),
                a.getAccountHolder(),
                a.getAccountNumber(),
                a.getAccountType(),
                a.getBranchCode(),
                a.getVatNumber()
        );
    }
}
