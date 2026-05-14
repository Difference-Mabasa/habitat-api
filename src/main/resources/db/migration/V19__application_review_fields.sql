-- V19: Free-text note the landlord attaches when reviewing an
-- application (decision rationale, conditions of approval, reason for
-- hold, etc.). Surfaces back to the tenant on /my-apps so a rejected
-- application carries a "why" line instead of going silent.

ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS decision_note TEXT,
    ADD COLUMN IF NOT EXISTS decided_at    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS decided_by    UUID REFERENCES users(id);
