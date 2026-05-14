-- V16: Tenant applications for a unit. Mirrors backroom-api's
-- applications table but trimmed to the fields habitat actually
-- consumes today; new statuses or scoring columns get added in
-- later migrations rather than re-shipping V16.

CREATE TABLE IF NOT EXISTS applications (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id             UUID         NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    tenant_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              VARCHAR(40)  NOT NULL DEFAULT 'SUBMITTED',
    message             TEXT,
    move_in_date        DATE,
    employment_status   VARCHAR(40),
    -- Audit columns required by BaseEntity (CreatedDate / LastModifiedDate
    -- + CreatedBy / LastModifiedBy + soft-delete pattern shared with V9 /
    -- properties).
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID,
    updated_by          UUID,
    deleted_at          TIMESTAMPTZ
);

-- One application per tenant per unit. Re-applying is an UPDATE on the
-- existing row in a later slice, not a duplicate row. Scoped to
-- non-soft-deleted rows so a previously-deleted application doesn't
-- block a re-apply.
CREATE UNIQUE INDEX IF NOT EXISTS uq_applications_tenant_unit
    ON applications (tenant_id, unit_id) WHERE deleted_at IS NULL;

-- Hot path: a tenant browsing their own application list.
CREATE INDEX IF NOT EXISTS idx_applications_tenant
    ON applications (tenant_id, created_at DESC) WHERE deleted_at IS NULL;

-- Hot path: a landlord browsing applications for one of their units.
CREATE INDEX IF NOT EXISTS idx_applications_unit
    ON applications (unit_id, created_at DESC) WHERE deleted_at IS NULL;
