-- Captures the typed-name e-signature on the online approve flow.
-- Both columns are populated together by MandateService.approveByLandlord
-- when a landlord clicks Approve in the inbox and confirms by retyping
-- their registered name. Nullable: pre-slice-2 ACTIVE mandates won't
-- have them, and the offline signing flow uses signed_document_path
-- instead.
ALTER TABLE mandates
    ADD COLUMN signed_name VARCHAR(120),
    ADD COLUMN signed_at   TIMESTAMPTZ;
