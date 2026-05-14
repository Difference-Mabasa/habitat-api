-- V21: Residential lease agreement generated when a deposit invoice is
-- paid. One lease per application; both parties (landlord + tenant)
-- sign before the application reaches COMPLETED in slice 4.
--
-- Template is one of the three SA-law-compliant templates the design
-- ships: RHA_STANDARD (12 month), RHA_SIX_MONTH, RHA_ROOM. Stored as a
-- short string so future templates can be added without a schema
-- change.

CREATE TABLE IF NOT EXISTS leases (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
    template            VARCHAR(40)  NOT NULL DEFAULT 'RHA_STANDARD',
    monthly_rent        NUMERIC(12, 2) NOT NULL,
    deposit             NUMERIC(12, 2) NOT NULL,
    term_months         INTEGER      NOT NULL DEFAULT 12,
    start_date          DATE,
    status              VARCHAR(40)  NOT NULL DEFAULT 'PENDING_SIGNATURES',
    lease_ref           VARCHAR(40)  NOT NULL UNIQUE,
    tenant_signed_at    TIMESTAMPTZ,
    landlord_signed_at  TIMESTAMPTZ,
    decline_reason      TEXT,
    signed_pdf_url      VARCHAR(500),
    -- BaseEntity audit + soft-delete.
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    deleted_at          TIMESTAMPTZ
);
