package com.habitat.api.repository;

import com.habitat.api.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    /** The (at most one) bank account attached to the given property. */
    Optional<BankAccount> findByProperty_Id(UUID propertyId);
}
