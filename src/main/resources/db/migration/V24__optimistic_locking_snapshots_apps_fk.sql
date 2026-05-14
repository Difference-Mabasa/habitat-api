-- V24 (Tech-debt sweep): closes BUG-01, BUG-02, BUG-03, ARCH-04 from
-- habitat-api/TECH_DEBT.md in a single migration. Larger than usual but
-- the items are tightly related (durable-record hygiene + optimistic
-- locking the same lifecycle tables).
--
--  BUG-01  @Version column on every BaseEntity table (Hibernate
--          optimistic locking — prevents races on review/pay/sign).
--  BUG-02  Snapshot columns on leases + invoices so durable records
--          freeze upstream identity at issuance time.
--  BUG-03  applications.tenant_id / unit_id flipped from CASCADE to
--          RESTRICT; application history survives user/unit deletes.
--  ARCH-04 applications.decided_by_name snapshot column.

-- ── BUG-01: version columns ─────────────────────────────────────────
-- Default 0 on existing rows; Hibernate auto-increments on each write.
-- One column per BaseEntity-extending table.

ALTER TABLE users                       ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE notifications               ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE notification_preferences    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE properties                  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE units                       ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE property_images             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE unit_images                 ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE applications                ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE application_documents       ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE property_required_documents ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE invoices                    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE leases                      ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ── BUG-02: snapshot columns on durable records ─────────────────────
-- Populated at issuance time, NEVER updated. Reads prefer snapshot;
-- the live foreign keys are kept for joins, but they no longer drive
-- the displayed identity (so a tenant changing their surname doesn't
-- rewrite their 2024 lease).

ALTER TABLE leases
    ADD COLUMN IF NOT EXISTS tenant_name_snapshot     VARCHAR(200),
    ADD COLUMN IF NOT EXISTS landlord_name_snapshot   VARCHAR(200),
    ADD COLUMN IF NOT EXISTS unit_title_snapshot      VARCHAR(200),
    ADD COLUMN IF NOT EXISTS property_title_snapshot  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS property_address_snapshot TEXT;

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS tenant_name_snapshot     VARCHAR(200),
    ADD COLUMN IF NOT EXISTS property_title_snapshot  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS property_address_snapshot TEXT;

-- Backfill from current live values for rows already in the table.
-- Snapshots written from now on flow through the service layer.

UPDATE leases l
   SET tenant_name_snapshot     = TRIM(BOTH ' ' FROM COALESCE(t.first_name,'') || ' ' || COALESCE(t.surname,'')),
       landlord_name_snapshot   = TRIM(BOTH ' ' FROM COALESCE(ll.first_name,'') || ' ' || COALESCE(ll.surname,'')),
       unit_title_snapshot      = u.title,
       property_title_snapshot  = p.title,
       property_address_snapshot = CONCAT_WS(', ',
                                             NULLIF(p.address_line, ''),
                                             NULLIF(p.suburb, ''),
                                             NULLIF(p.city, ''),
                                             NULLIF(p.postal_code, ''))
  FROM users      t,
       users      ll,
       units      u,
       properties p
 WHERE t.id  = l.tenant_id
   AND ll.id = l.landlord_id
   AND u.id  = l.unit_id
   AND p.id  = l.property_id
   AND l.tenant_name_snapshot IS NULL;

UPDATE invoices i
   SET tenant_name_snapshot     = TRIM(BOTH ' ' FROM COALESCE(t.first_name,'') || ' ' || COALESCE(t.surname,'')),
       property_title_snapshot  = p.title,
       property_address_snapshot = CONCAT_WS(', ',
                                             NULLIF(p.address_line, ''),
                                             NULLIF(p.suburb, ''),
                                             NULLIF(p.city, ''),
                                             NULLIF(p.postal_code, ''))
  FROM users      t,
       properties p
 WHERE t.id = i.tenant_id
   AND p.id = i.property_id
   AND i.tenant_name_snapshot IS NULL;

-- ── BUG-03: applications FK flips ───────────────────────────────────
-- Application history matters for fair-housing audits. Don't lose it
-- when a user / unit is hard-deleted.

ALTER TABLE applications DROP CONSTRAINT IF EXISTS applications_tenant_id_fkey;
ALTER TABLE applications
    ADD CONSTRAINT applications_tenant_id_fkey
        FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE applications DROP CONSTRAINT IF EXISTS applications_unit_id_fkey;
ALTER TABLE applications
    ADD CONSTRAINT applications_unit_id_fkey
        FOREIGN KEY (unit_id) REFERENCES units(id) ON DELETE RESTRICT;

-- ── ARCH-04: decided_by_name snapshot ───────────────────────────────
-- decided_by is just a UUID; if a landlord is hard-deleted their name
-- vanishes from the audit trail. Snapshot the display name at decision
-- time. NULL on rows reviewed before this migration — that's fine.

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS decided_by_name VARCHAR(200);
