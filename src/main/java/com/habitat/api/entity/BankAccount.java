package com.habitat.api.entity;

import com.habitat.api.entity.base.BaseEntity;
import com.habitat.api.enums.BankAccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Payout destination for a single {@link Property}. One bank row per
 * property — an agent listing on behalf of an offline landlord captures
 * the LANDLORD'S bank here, not their own. A user's personal earnings
 * (agent fees, tenant refunds) belong on a separate per-user record.
 *
 * Phase 8 (Payouts) will own the ledger that moves money TO this row;
 * for now the wizard's step-5 form persists into it so payout details
 * land alongside the listing and never bleed across drafts.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bank_accounts")
@SQLRestriction("deleted_at IS NULL")
public class BankAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false, unique = true)
    private Property property;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_holder", nullable = false, length = 200)
    private String accountHolder;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private BankAccountType accountType;

    @Column(name = "branch_code", nullable = false, length = 10)
    private String branchCode;

    @Column(name = "vat_number", length = 20)
    private String vatNumber;
}
