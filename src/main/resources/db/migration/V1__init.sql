-- Habitat API — initial schema.
-- Lessons applied:
--   * Every table has UUID id + audit columns + soft-delete column.
--   * Every enum value lives in a Postgres ENUM TYPE so adding a value
--     uses ALTER TYPE ... ADD VALUE — never rename existing values.
--   * FK columns are indexed up front; common lookup columns too.

-- ─────────────────────────────────────────────────────────────────────────
-- ENUM TYPES
-- ─────────────────────────────────────────────────────────────────────────

CREATE TYPE user_role AS ENUM (
    'TENANT',
    'LANDLORD',
    'AGENT',
    'ADMIN',
    'SUPER_ADMIN'
);

-- ─────────────────────────────────────────────────────────────────────────
-- users
-- ─────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255),
    display_name    VARCHAR(80)  NOT NULL,
    active_role     VARCHAR(32)  NOT NULL,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    area            VARCHAR(80),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);

-- Case-insensitive uniqueness on email + indexes for hot reads.
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users (LOWER(email))
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_users_active_role ON users (active_role) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_users_area        ON users (area)        WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────
-- user_roles  (Set<Role> per user — drives the workspace dropdown)
-- ─────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_roles (
    user_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role     VARCHAR(32)  NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE INDEX IF NOT EXISTS ix_user_roles_user ON user_roles (user_id);

-- ─────────────────────────────────────────────────────────────────────────
-- ShedLock table — distributed scheduler lock from day one
-- ─────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- ─────────────────────────────────────────────────────────────────────────
-- webhook_events — every external webhook is persisted before processing
-- ─────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS webhook_events (
    id            UUID         PRIMARY KEY,
    provider      VARCHAR(32)  NOT NULL,                  -- e.g. 'ozow'
    external_id   VARCHAR(128) NOT NULL,                  -- provider's idempotency key
    payload       JSONB        NOT NULL,
    received_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    source_ip     VARCHAR(64),
    processed_at  TIMESTAMPTZ,
    UNIQUE (provider, external_id)
);

CREATE INDEX IF NOT EXISTS ix_webhook_provider_received
    ON webhook_events (provider, received_at DESC);
