package com.habitat.api.service;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.dto.user.BankAccountResponse;
import com.habitat.api.dto.user.UpsertBankAccountRequest;
import com.habitat.api.entity.BankAccount;
import com.habitat.api.entity.Property;
import com.habitat.api.exception.ResourceNotFoundException;
import com.habitat.api.repository.BankAccountRepository;
import com.habitat.api.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Per-property payout destination. Read/write requires
 * {@link PropertyService#canEdit(Property)} — same authorization as
 * any other property mutation, so the property's manager (and, for
 * online-owner properties, the landlord-user themselves) can manage
 * the bank but no one else.
 *
 * Upsert by convention. Partial PATCHes on bank details are a
 * footgun — the wizard either has every required field or it doesn't
 * call this endpoint at all.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final BankAccountRepository accounts;
    private final PropertyRepository properties;
    private final PropertyService propertyService;

    /** {@link Optional#empty()} when no bank has been set on the property yet. */
    @Transactional(readOnly = true)
    public Optional<BankAccountResponse> getForProperty(UUID propertyId) {
        Property property = properties.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PROPERTY_NOT_FOUND));
        propertyService.requireCanEdit(property);
        return accounts.findByProperty_Id(propertyId).map(BankAccountResponse::from);
    }

    @Transactional
    public BankAccountResponse upsertForProperty(UUID propertyId, UpsertBankAccountRequest req) {
        Property property = properties.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.PROPERTY_NOT_FOUND));
        propertyService.requireCanEdit(property);
        BankAccount account = accounts.findByProperty_Id(propertyId)
                .orElseGet(() -> BankAccount.builder().property(property).build());
        account.setBankName(req.bankName());
        account.setAccountHolder(req.accountHolder());
        account.setAccountNumber(req.accountNumber());
        account.setAccountType(req.accountType());
        account.setBranchCode(req.branchCode());
        account.setVatNumber(req.vatNumber());
        BankAccount saved = accounts.save(account);
        log.info("upserted bank account for property {}", propertyId);
        return BankAccountResponse.from(saved);
    }
}
