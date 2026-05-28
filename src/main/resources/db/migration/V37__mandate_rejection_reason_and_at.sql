-- Captures the rejection reason + timestamp when the online landlord
-- rejects a mandate from /mandate-approvals. Both columns are populated
-- together by MandateService.rejectByLandlord. Required client-side
-- (and re-validated server-side via the RejectMandateRequest DTO) so
-- the agent has actionable feedback before slice 4's revise/resubmit
-- round-trip lands. Nullable: pre-slice-3 REJECTED rows won't have
-- them; the UI banner falls back to a generic "Was rejected" message.
ALTER TABLE mandates
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN rejected_at      TIMESTAMPTZ;
