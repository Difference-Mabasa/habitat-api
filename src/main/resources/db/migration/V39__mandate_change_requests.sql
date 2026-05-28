-- Slice 4: structured request-changes round-trip.
--
-- Each row captures one round of the landlord asking the agent to
-- revise the mandate before signing. items is a jsonb array of
-- {field, currentValue, requestedValue} — typed for the "Apply
-- suggested" one-click path on the agent's resolution panel.
-- comment is the freeform "anything else" the structured items
-- can't capture.
--
-- Status workflow:
--   OPEN       — landlord just requested; awaiting agent action
--   ADDRESSED  — agent resubmitted (mandate is back at PENDING_LANDLORD_APPROVAL)
--   WITHDRAWN  — the landlord approved/rejected anyway, or the agent withdrew
CREATE TABLE IF NOT EXISTS mandate_change_requests (
    id                       UUID         PRIMARY KEY,
    mandate_id               UUID         NOT NULL,
    requested_by_user_id     UUID         NOT NULL,
    requested_at             TIMESTAMPTZ  NOT NULL,
    comment                  TEXT,
    items                    JSONB        NOT NULL DEFAULT '[]'::jsonb,
    status                   VARCHAR(40)  NOT NULL,
    resolved_at              TIMESTAMPTZ,
    resolved_by_user_id      UUID,
    -- BaseEntity columns
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT change_requests_mandate_fk
        FOREIGN KEY (mandate_id)           REFERENCES mandates(id) ON DELETE RESTRICT,
    CONSTRAINT change_requests_requester_fk
        FOREIGN KEY (requested_by_user_id) REFERENCES users(id)    ON DELETE RESTRICT,
    CONSTRAINT change_requests_resolver_fk
        FOREIGN KEY (resolved_by_user_id)  REFERENCES users(id)    ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_change_requests_mandate ON mandate_change_requests (mandate_id)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_change_requests_status  ON mandate_change_requests (status)
    WHERE deleted_at IS NULL;
