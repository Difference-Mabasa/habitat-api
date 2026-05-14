-- V25: Apply the standard SA-tenant document set to every property
-- that doesn't already have a required-document checklist. Lets the
-- apply flow's AWAITING_DOCUMENTS branch be exercised against any
-- listing in the catalogue, not only the 5 hand-picked in V18.
--
-- Standard set: SA_ID + PAYSLIPS_3_MONTHS + BANK_STATEMENTS_3_MONTHS
-- + PROOF_OF_ADDRESS. Skips EMPLOYMENT_LETTER (employer-dependent;
-- landlords can opt-in per property later).
--
-- Idempotent: only inserts for (property, doc_type) pairs that don't
-- already exist, so re-running against a partially-seeded DB doesn't
-- duplicate rows.

INSERT INTO property_required_documents (id, property_id, doc_type, created_at, updated_at)
SELECT gen_random_uuid(), p.id, dt.doc_type, NOW(), NOW()
FROM properties p
CROSS JOIN (VALUES
    ('SA_ID'),
    ('PAYSLIPS_3_MONTHS'),
    ('BANK_STATEMENTS_3_MONTHS'),
    ('PROOF_OF_ADDRESS')
) AS dt(doc_type)
WHERE p.deleted_at IS NULL
  AND NOT EXISTS (
        SELECT 1
        FROM property_required_documents prd
        WHERE prd.property_id = p.id
          AND prd.doc_type    = dt.doc_type
          AND prd.deleted_at IS NULL
  );
