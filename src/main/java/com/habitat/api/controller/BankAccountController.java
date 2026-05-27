package com.habitat.api.controller;

import com.habitat.api.constants.ApiRoutes;
import com.habitat.api.dto.user.BankAccountResponse;
import com.habitat.api.dto.user.UpsertBankAccountRequest;
import com.habitat.api.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Per-property payout endpoints.
 *
 * <p>{@code GET /properties/{propertyId}/bank-account} → 200 with the
 * row, or 404 when no bank has been set on this property yet. The
 * wizard treats 404 as "fields stay blank" rather than an error.
 *
 * <p>{@code PUT /properties/{propertyId}/bank-account} → upsert.
 * Single endpoint because partial bank-detail PATCHes are a footgun.
 *
 * <p>Authorization on both is delegated to
 * {@code PropertyService.canEdit} — the property's manager (and,
 * for online-owner listings, the landlord-user themselves) can read
 * and write; anyone else gets 403.
 */
@RestController
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService accounts;

    @GetMapping(ApiRoutes.PROPERTIES_BANK_ACCOUNT)
    public ResponseEntity<BankAccountResponse> getForProperty(@PathVariable UUID propertyId) {
        return accounts.getForProperty(propertyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(ApiRoutes.PROPERTIES_BANK_ACCOUNT)
    public BankAccountResponse upsertForProperty(
            @PathVariable UUID propertyId,
            @Valid @RequestBody UpsertBankAccountRequest req) {
        return accounts.upsertForProperty(propertyId, req);
    }
}
