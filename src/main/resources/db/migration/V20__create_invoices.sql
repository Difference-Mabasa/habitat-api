-- V20: Deposit + first-month-rent invoices generated when a landlord
-- approves a tenant's application. One invoice per application — paying
-- it flips the application status to DEPOSIT_PAID, which unblocks the
-- lease-generation slice.
--
-- Amounts mirror backroom: deposit defaults to one month's rent;
-- first_month_rent is captured separately so future fees / utilities
-- riders can extend the schema without redoing this migration.

CREATE TABLE IF NOT EXISTS invoices (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
    tenant_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    deposit_amount      NUMERIC(12, 2) NOT NULL,
    first_month_rent    NUMERIC(12, 2),
    total_amount        NUMERIC(12, 2) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    invoice_ref         VARCHAR(40)  NOT NULL UNIQUE,
    issued_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ,
    paid_at             TIMESTAMPTZ,
    payment_reference   VARCHAR(255),
    -- BaseEntity audit columns + soft-delete.
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    deleted_at          TIMESTAMPTZ
);

-- Hot path: a tenant browsing their own outstanding invoices.
CREATE INDEX IF NOT EXISTS idx_invoices_tenant
    ON invoices (tenant_id, created_at DESC) WHERE deleted_at IS NULL;
