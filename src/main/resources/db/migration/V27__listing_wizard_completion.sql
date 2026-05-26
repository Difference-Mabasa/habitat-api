-- V27: complete the landlord listing wizard (Phase 4 close).
--
-- Three additive surfaces. All nullable on existing rows — none of the
-- following backfills retroactively rewrite seeded data.
--
--   1. properties.listing_mode + mandate_fee_percent
--      Captures the wizard's "Mandate" step inline on the property.
--      LANDLORD_DIRECT (default) means the landlord runs the listing
--      themselves; AGENT_MANAGED means an agent placed the mandate
--      and earns the recorded fee percent. The full mandate-approval
--      state machine lands in Phase 12 — for now these two columns
--      hold the user-supplied facts the wizard already collected.
--
--   2. bank_accounts table, one-to-one with users
--      Landlords need somewhere for rent to land. Phase 8 (Payouts)
--      will model the transfer ledger; this table just lets the
--      wizard's "Payout details" step persist real data today. Per
--      user, not per property — habitat-ui's wizard collects bank
--      details once on the user, not on each listing.
--
--   3. property_images.cover-image semantics already exist (is_cover
--      + sort_order). No schema change needed — Phase 4 wiring
--      reuses the columns landed in V9. The upload endpoints land
--      in this commit alongside V27 so callers can populate them.

ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS listing_mode        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS mandate_fee_percent NUMERIC(5, 2);

-- Default existing rows to LANDLORD_DIRECT so older listings keep
-- the "landlord runs this" semantic. AGENT_MANAGED is opt-in by the
-- wizard.
UPDATE properties
   SET listing_mode = 'LANDLORD_DIRECT'
 WHERE listing_mode IS NULL;

CREATE TABLE IF NOT EXISTS bank_accounts (
    id              UUID         PRIMARY KEY,
    user_id         UUID         NOT NULL UNIQUE,
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
    CONSTRAINT bank_accounts_user_fk
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS bank_accounts_user_id_idx
    ON bank_accounts (user_id)
 WHERE deleted_at IS NULL;
