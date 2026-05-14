-- V18: Required documents on a handful of seeded properties. Picks the
-- premium V10 listings so the apply demo flow shows the
-- AWAITING_DOCUMENTS branch end-to-end without every test apply on the
-- catalogue triggering an upload screen.
--
-- Selection: 5 properties get the full SA-standard ask
-- (SA_ID + PAYSLIPS_3_MONTHS + PROOF_OF_ADDRESS). Everything else
-- stays document-free, so an apply against any V11/V12 Midrand block
-- lands at SUBMITTED.

INSERT INTO property_required_documents (id, property_id, doc_type, created_at, updated_at)
VALUES
    (gen_random_uuid(), md5('hab-prop-1')::uuid,  'SA_ID',               NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-1')::uuid,  'PAYSLIPS_3_MONTHS',   NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-1')::uuid,  'PROOF_OF_ADDRESS',    NOW(), NOW()),

    (gen_random_uuid(), md5('hab-prop-2')::uuid,  'SA_ID',               NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-2')::uuid,  'PAYSLIPS_3_MONTHS',   NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-2')::uuid,  'PROOF_OF_ADDRESS',    NOW(), NOW()),

    (gen_random_uuid(), md5('hab-prop-4')::uuid,  'SA_ID',               NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-4')::uuid,  'PAYSLIPS_3_MONTHS',   NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-4')::uuid,  'EMPLOYMENT_LETTER',   NOW(), NOW()),

    (gen_random_uuid(), md5('hab-prop-8')::uuid,  'SA_ID',               NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-8')::uuid,  'PAYSLIPS_3_MONTHS',   NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-8')::uuid,  'BANK_STATEMENTS_3_MONTHS', NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-8')::uuid,  'PROOF_OF_ADDRESS',    NOW(), NOW()),

    (gen_random_uuid(), md5('hab-prop-13')::uuid, 'SA_ID',               NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-13')::uuid, 'PAYSLIPS_3_MONTHS',   NOW(), NOW()),
    (gen_random_uuid(), md5('hab-prop-13')::uuid, 'PROOF_OF_ADDRESS',    NOW(), NOW())
ON CONFLICT DO NOTHING;
