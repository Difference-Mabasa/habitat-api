-- V35: re-key bank_accounts from user_id → property_id.
--
-- V27 created bank_accounts with a UNIQUE constraint on user_id, on the
-- (wrong, in hindsight) assumption that one user has one payout
-- destination across every listing they manage. That conflates two
-- distinct concepts:
--
--   * A user's own earnings — agent fees, refunds owed to them as a
--     tenant, etc. Lives on the user.
--   * A property's rent destination — where rent for THIS listing
--     lands. Owned by the landlord on the property, not the user
--     managing it. An agent listing on behalf of an offline landlord
--     must capture the LANDLORD'S bank, not their own.
--
-- The bank_accounts table only ever held wizard-stage data (V27 is
-- explicit about this), no production rows. Cleanest path is to drop
-- and recreate keyed on property_id. Per-user bank accounts (for agent
-- fees / refunds) will get a separate table when that feature lands.
--
-- FK ON DELETE RESTRICT — properties soft-delete via deleted_at, and a
-- hard property delete with a live bank row would leave the row
-- dangling; force the caller to remove the bank row first if they
-- really mean a hard delete.

DROP TABLE IF EXISTS bank_accounts;

CREATE TABLE bank_accounts (
    id              UUID         PRIMARY KEY,
    property_id     UUID         NOT NULL UNIQUE,
    bank_name       VARCHAR(100) NOT NULL,
    account_holder  VARCHAR(200) NOT NULL,
    account_number  VARCHAR(50)  NOT NULL,
    account_type    VARCHAR(30)  NOT NULL,
    branch_code     VARCHAR(10)  NOT NULL,
    vat_number      VARCHAR(20),
    -- BaseEntity columns
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT bank_accounts_property_fk
        FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE RESTRICT
);

CREATE INDEX bank_accounts_property_id_idx
    ON bank_accounts (property_id)
 WHERE deleted_at IS NULL;
