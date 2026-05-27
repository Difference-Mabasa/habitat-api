-- V33: tenant viewing requests. State machine in
-- enums/ViewingStatus.java; comms wired via ViewingService events.
-- Indexes mirror PropertyRepository's join keys — tenant-side
-- lookups (GET /viewings/mine) and manager-side
-- (GET /viewings/managed via unit→property→manager).

CREATE TABLE IF NOT EXISTS viewings (
    id              UUID         PRIMARY KEY,
    unit_id         UUID         NOT NULL,
    tenant_user_id  UUID         NOT NULL,
    scheduled_at    TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    notes           TEXT,
    decision_note   TEXT,
    decided_at      TIMESTAMPTZ,
    decided_by      UUID,
    cancelled_by    UUID,
    -- BaseEntity columns
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT viewings_unit_fk
        FOREIGN KEY (unit_id)        REFERENCES units(id) ON DELETE RESTRICT,
    CONSTRAINT viewings_tenant_fk
        FOREIGN KEY (tenant_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS viewings_tenant_idx
    ON viewings (tenant_user_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS viewings_unit_idx
    ON viewings (unit_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS viewings_scheduled_idx
    ON viewings (scheduled_at) WHERE deleted_at IS NULL;
