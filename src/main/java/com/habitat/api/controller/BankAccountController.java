package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.user.BankAccountResponse;
import com.habitat.api.dto.user.UpsertBankAccountRequest;
import com.habitat.api.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payout-destination endpoints, scoped to the current user.
 *
 * <p>{@code GET /users/me/bank-account} → 200 with the current row,
 * or 404 when none is set yet. The wizard treats 404 as "ask the
 * user to enter details" rather than "error".
 *
 * <p>{@code PUT /users/me/bank-account} → upsert. Single endpoint
 * because partial bank-detail PATCHes are a footgun.
 */
@RestController
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService accounts;

    @GetMapping(ApiRoutes.USERS_ME_BANK_ACCOUNT)
    public ResponseEntity<BankAccountResponse> getMine() {
        return accounts.getMine()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(ApiRoutes.USERS_ME_BANK_ACCOUNT)
    public BankAccountResponse upsertMine(@Valid @RequestBody UpsertBankAccountRequest req) {
        return accounts.upsertMine(req);
    }
}
