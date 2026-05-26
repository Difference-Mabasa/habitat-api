package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.user.BankAccountResponse;
import com.habitat.api.dto.user.UpsertBankAccountRequest;
import com.habitat.api.entity.BankAccount;
import com.habitat.api.entity.User;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.BankAccountRepository;
import com.habitat.api.repository.UserRepository;
import com.habitat.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Payout-destination lookup for the current user. Upsert by convention
 * — one bank account per landlord today; future per-property variants
 * can extend the entity without changing this API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final BankAccountRepository accounts;
    private final UserRepository users;
    private final SecurityUtils security;

    /** {@link Optional#empty()} when the user hasn't set one yet. */
    @Transactional(readOnly = true)
    public Optional<BankAccountResponse> getMine() {
        UUID me = security.requireUserId();
        return accounts.findByUser_Id(me).map(BankAccountResponse::from);
    }

    @Transactional
    public BankAccountResponse upsertMine(UpsertBankAccountRequest req) {
        UUID me = security.requireUserId();
        BankAccount account = accounts.findByUser_Id(me).orElseGet(() -> {
            User user = users.findById(me)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND));
            return BankAccount.builder().user(user).build();
        });
        account.setBankName(req.bankName());
        account.setAccountHolder(req.accountHolder());
        account.setAccountNumber(req.accountNumber());
        account.setAccountType(req.accountType());
        account.setBranchCode(req.branchCode());
        account.setVatNumber(req.vatNumber());
        BankAccount saved = accounts.save(account);
        log.info("upserted bank account for user {}", me);
        return BankAccountResponse.from(saved);
    }
}
