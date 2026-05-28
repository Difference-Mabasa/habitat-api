-- Slice 4: distinguish the landlord-reject path (slice 3) from the
-- agent-withdraw path (slice 4). Both flip status to REJECTED but
-- the actor differs, and the UI copy + audit trail on the detail
-- screen need to tell them apart.
--
--   rejection_reason / rejected_at     (V37) — landlord rejected
--   withdrawn_reason / withdrawn_at    (here) — agent withdrew
--   rejected_by_user_id                — landlord on V37 path; null otherwise
--   withdrawn_by_user_id               — agent on withdraw path; null otherwise
--
-- All four are nullable; only the relevant pair is populated per
-- transition. Pre-slice-4 REJECTED rows leave all four null and the
-- UI falls back to the slice-3 banner.
ALTER TABLE mandates
    ADD COLUMN IF NOT EXISTS rejected_by_user_id  UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS withdrawn_reason     TEXT,
    ADD COLUMN IF NOT EXISTS withdrawn_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS withdrawn_by_user_id UUID REFERENCES users(id);
