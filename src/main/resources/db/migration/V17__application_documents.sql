-- V17: Per-property required documents + per-application uploaded
-- documents. Mirrors backroom-api's PropertyRequiredDocument +
-- ApplicationDocument shape, scoped to what habitat consumes today.
--
-- Two new tables:
--   property_required_documents — landlord declares "applicants need
--     payslips + ID + proof of address" against a property
--   application_documents — tenant uploads against an application,
--     keyed on docType (one row per type per application)
--
-- ApplicationStatus enum widens: AWAITING_DOCUMENTS + DOCUMENTS_SUBMITTED.
-- Enums on this side are STRING-stored, so no PostgreSQL enum type to
-- ALTER — habits Java side handles the new values.

CREATE TABLE IF NOT EXISTS property_required_documents (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id   UUID         NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    doc_type      VARCHAR(40)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    UUID,
    updated_by    UUID,
    deleted_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_required_docs_property_type
    ON property_required_documents (property_id, doc_type) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS application_documents (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID         NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    doc_type        VARCHAR(40)  NOT NULL,
    -- File metadata. file_url is a relative path served by a static
    -- handler once the real StorageService lands; for now the apply
    -- flow records a stub URL containing the original filename.
    file_url        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ
);

-- One uploaded doc per type per application. Re-uploading overwrites the
-- existing row (handled service-side).
CREATE UNIQUE INDEX IF NOT EXISTS uq_app_docs_application_type
    ON application_documents (application_id, doc_type) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_app_docs_application
    ON application_documents (application_id) WHERE deleted_at IS NULL;
