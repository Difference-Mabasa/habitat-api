package com.habitat.api.dto.user;

import com.habitat.api.enums.BankAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code PUT /users/me/bank-account}. Upsert semantics — if
 * the user already has a row, all fields are overwritten; otherwise
 * a new row is created. There is intentionally no per-field PATCH:
 * partial bank details are a footgun.
 */
public record UpsertBankAccountRequest(
        @NotBlank @Size(max = 100) String bankName,
        @NotBlank @Size(max = 200) String accountHolder,
        @NotBlank @Size(max = 50)  String accountNumber,
        @NotNull BankAccountType accountType,
        @NotBlank @Size(max = 10)  String branchCode,
        @Size(max = 20) String vatNumber
) {}
