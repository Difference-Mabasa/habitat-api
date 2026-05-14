-- V22 (Tier A corrective): leases + invoices get their own party
-- identity so they survive their parent application being archived /
-- hard-deleted. Application becomes a nullable trace pointer.
--
-- Before V22:
--   leases.application_id   NOT NULL UNIQUE ON DELETE CASCADE
--   invoices.application_id NOT NULL UNIQUE ON DELETE CASCADE
--   invoices.tenant_id      NOT NULL ON DELETE CASCADE
--   ↳ both rows vanished if the application or user was hard-deleted
--     — unacceptable for a legal contract / accounting record.
--
-- After V22:
--   leases now carries tenant_id + landlord_id + unit_id + property_id
--   invoices now carries unit_id + property_id + landlord_id
--   ↳ FKs to those tables use ON DELETE RESTRICT — you can't drop a
--     user / unit / property with live leases or invoices.
--   application_id becomes nullable with ON DELETE SET NULL — kept as
--     a trace pointer for "which application generated this lease",
--     but its loss doesn't destroy the durable record.

-- ─── Leases ──────────────────────────────────────────────────────────

ALTER TABLE leases
    ADD COLUMN IF NOT EXISTS tenant_id   UUID,
    ADD COLUMN IF NOT EXISTS landlord_id UUID,
    ADD COLUMN IF NOT EXISTS unit_id     UUID,
    ADD COLUMN IF NOT EXISTS property_id UUID;

UPDATE leases l
   SET tenant_id   = a.tenant_id,
       landlord_id = p.manager_id,
       unit_id     = a.unit_id,
       property_id = p.id
  FROM applications a
  JOIN units      u ON u.id = a.unit_id
  JOIN properties p ON p.id = u.property_id
 WHERE a.id = l.application_id
   AND l.tenant_id IS NULL;

ALTER TABLE leases
    ALTER COLUMN tenant_id   SET NOT NULL,
    ALTER COLUMN landlord_id SET NOT NULL,
    ALTER COLUMN unit_id     SET NOT NULL,
    ALTER COLUMN property_id SET NOT NULL,
    ALTER COLUMN application_id DROP NOT NULL;

ALTER TABLE leases DROP CONSTRAINT IF EXISTS leases_application_id_fkey;
ALTER TABLE leases
    ADD CONSTRAINT leases_application_id_fkey
        FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE SET NULL;

ALTER TABLE leases
    ADD CONSTRAINT leases_tenant_fk
        FOREIGN KEY (tenant_id)   REFERENCES users(id)      ON DELETE RESTRICT,
    ADD CONSTRAINT leases_landlord_fk
        FOREIGN KEY (landlord_id) REFERENCES users(id)      ON DELETE RESTRICT,
    ADD CONSTRAINT leases_unit_fk
        FOREIGN KEY (unit_id)     REFERENCES units(id)      ON DELETE RESTRICT,
    ADD CONSTRAINT leases_property_fk
        FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_leases_tenant
    ON leases (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_leases_landlord
    ON leases (landlord_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_leases_unit
    ON leases (unit_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_leases_property
    ON leases (property_id) WHERE deleted_at IS NULL;

-- ─── Invoices ────────────────────────────────────────────────────────

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS unit_id     UUID,
    ADD COLUMN IF NOT EXISTS property_id UUID,
    ADD COLUMN IF NOT EXISTS landlord_id UUID;

UPDATE invoices i
   SET unit_id     = a.unit_id,
       property_id = p.id,
       landlord_id = p.manager_id
  FROM applications a
  JOIN units      u ON u.id = a.unit_id
  JOIN properties p ON p.id = u.property_id
 WHERE a.id = i.application_id
   AND i.unit_id IS NULL;

ALTER TABLE invoices
    ALTER COLUMN unit_id     SET NOT NULL,
    ALTER COLUMN property_id SET NOT NULL,
    ALTER COLUMN landlord_id SET NOT NULL,
    ALTER COLUMN application_id DROP NOT NULL;

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_application_id_fkey;
ALTER TABLE invoices
    ADD CONSTRAINT invoices_application_id_fkey
        FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE SET NULL;

-- Replace the destructive CASCADE on tenant_id with RESTRICT —
-- accounting records can't disappear when a user account is closed.
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_tenant_id_fkey;
ALTER TABLE invoices
    ADD CONSTRAINT invoices_tenant_id_fkey
        FOREIGN KEY (tenant_id)   REFERENCES users(id)      ON DELETE RESTRICT,
    ADD CONSTRAINT invoices_landlord_fk
        FOREIGN KEY (landlord_id) REFERENCES users(id)      ON DELETE RESTRICT,
    ADD CONSTRAINT invoices_unit_fk
        FOREIGN KEY (unit_id)     REFERENCES units(id)      ON DELETE RESTRICT,
    ADD CONSTRAINT invoices_property_fk
        FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_invoices_landlord
    ON invoices (landlord_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_invoices_unit
    ON invoices (unit_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_invoices_property
    ON invoices (property_id) WHERE deleted_at IS NULL;
