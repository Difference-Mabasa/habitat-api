-- V30: Landlord entity — distinct identity from User, captures legal
-- owners with or without a Habitat account (online vs offline). See
-- entity Landlord.java for the full rationale; backfill from existing
-- properties + mandates happens in V31.

CREATE TABLE IF NOT EXISTS landlords (
    id                    UUID         PRIMARY KEY,
    type                  VARCHAR(16)  NOT NULL,             -- ONLINE | OFFLINE
    user_id               UUID,                              -- NOT NULL when ONLINE
    id_number             VARCHAR(13),                       -- SA ID, Luhn-validated at app layer
    created_by_agent_id   UUID,                              -- NULL when row was born ONLINE
    first_name            VARCHAR(100),
    last_name             VARCHAR(100),
    email                 VARCHAR(255),
    phone                 VARCHAR(50),
    -- BaseEntity columns
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    created_by            UUID,
    updated_by            UUID,
    deleted_at            TIMESTAMPTZ,
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT landlords_user_fk
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT landlords_created_by_agent_fk
        FOREIGN KEY (created_by_agent_id) REFERENCES users(id) ON DELETE RESTRICT,
    -- ONLINE ⇒ user_id non-null + creator-agent null. OFFLINE ⇒
    -- inverse. Caught at the DB layer so a bad service path can't
    -- create a half-typed row.
    CONSTRAINT landlords_type_consistent CHECK (
        (type = 'ONLINE'  AND user_id IS NOT NULL) OR
        (type = 'OFFLINE' AND user_id IS NULL)
    )
);

-- One Landlord per User — so an owner with multiple properties shares
-- a single canonical row. Partial UNIQUE so OFFLINE rows (user_id null)
-- don't collide.
CREATE UNIQUE INDEX IF NOT EXISTS ux_landlords_user
    ON landlords (user_id)
    WHERE user_id IS NOT NULL AND deleted_at IS NULL;

-- Dedup key: agent capturing the same SA ID across two mandates lands
-- on one row. Race-safe (DB-level uniqueness, not just service check).
CREATE UNIQUE INDEX IF NOT EXISTS ux_landlords_id_number
    ON landlords (id_number)
    WHERE id_number IS NOT NULL AND deleted_at IS NULL;

-- Hot read: lookupByIdNumber is the wizard's step-2 dedup call.
CREATE INDEX IF NOT EXISTS ix_landlords_id_number
    ON landlords (id_number)
    WHERE deleted_at IS NULL;

-- Created-by-agent lookups (Phase 2 inbox: "your captured landlords").
CREATE INDEX IF NOT EXISTS ix_landlords_created_by_agent
    ON landlords (created_by_agent_id)
    WHERE deleted_at IS NULL;
