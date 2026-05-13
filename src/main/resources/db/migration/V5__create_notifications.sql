-- In-app notifications.
--
-- Aligned with backroom-api's notifications model with two upgrades baked in
-- from day one (the recon called them out as TECH_DEBT in backroom):
--   1. action_url + action_label are first-class columns. Backroom infers
--      the CTA target by matching the title string ("Welcome to Backroom!")
--      in the UI — fragile. Storing the CTA with the notification lets the
--      backend own it.
--   2. read_at is recorded alongside the boolean. Lets a scheduled job prune
--      acknowledged notifications after a TTL without losing the "when was
--      this dismissed" audit signal.

CREATE TABLE IF NOT EXISTS notifications (
    id            UUID         PRIMARY KEY,
    recipient_id  UUID         NOT NULL,
    type          VARCHAR(48)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    body          TEXT,
    ref_id        UUID,
    action_url    VARCHAR(255),
    action_label  VARCHAR(80),
    read          BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    created_by    UUID,
    updated_by    UUID,
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Per development-standards §7: index every FK + every WHERE-able column on a
-- table that grows. unread-count + paginated list reads dominate this table.
CREATE INDEX IF NOT EXISTS ix_notifications_recipient
    ON notifications (recipient_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_notifications_recipient_unread
    ON notifications (recipient_id)
    WHERE read = FALSE AND deleted_at IS NULL;
