-- Multi-channel notifications + per-user preferences.
--
-- The notifications table grows a `category` column — the preference unit
-- a user toggles (SYSTEM, ACCOUNT, ONBOARDING, MESSAGING, BILLING,
-- MARKETING). The fine-grained `type` (WELCOME, NEW_DEVICE_LOGIN, …)
-- stays on the row but no longer drives delivery routing on its own.
--
-- A new `notification_preferences` table records per-user opt-outs by
-- (category, channel). A missing row is treated as opted-in by the
-- resolver — only opt-outs leave a footprint. The (SYSTEM, IN_APP)
-- combination is enforced un-opt-outable in service code; we don't add
-- a DB check constraint because preference rows for that pair are
-- simply ignored at read time.

ALTER TABLE notifications
    ADD COLUMN category VARCHAR(32);

-- Backfill: every notification persisted under V5 was a WELCOME row.
-- WELCOME lives under the ONBOARDING category in the new taxonomy.
-- Anything we encounter that's not WELCOME (shouldn't exist yet) falls
-- through to SYSTEM as a safe default.
UPDATE notifications
SET category = CASE
                   WHEN type = 'WELCOME' THEN 'ONBOARDING'
                   ELSE 'SYSTEM'
               END
WHERE category IS NULL;

ALTER TABLE notifications
    ALTER COLUMN category SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_notifications_category
    ON notifications (category)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id    UUID         NOT NULL,
    category   VARCHAR(32)  NOT NULL,
    channel    VARCHAR(16)  NOT NULL,
    enabled    BOOLEAN      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (user_id, category, channel),
    CONSTRAINT fk_notification_prefs_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Read pattern: PreferencesService.getMatrix(userId) scans the user's
-- rows once. Single composite index covers the (user_id, category,
-- channel) lookup path already via the primary key.
