package com.habitat.api.repository;

import com.habitat.api.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    /** The (at most one) bank account owned by the given user. */
    Optional<BankAccount> findByUser_Id(UUID userId);
}
