-- V26: storage metadata for ApplicationDocument
--
-- Phase 7 (file storage + multipart) lands real bytes-on-disk for
-- application documents. Adds two metadata columns so the API can
-- expose detected MIME + size to the UI without re-reading the file:
--
--   mime_type   — Apache Tika magic-byte detection at upload time. NOT
--                 the client-supplied Content-Type header (that's
--                 spoofable, per development-standards.md §6).
--   size_bytes  — exact byte length as written. Hard cap is enforced
--                 in StorageService; this stores what actually landed.
--
-- Existing rows keep NULL until they're re-uploaded — the old fileUrl
-- column held a stub URL, not a real file, so back-filling is moot.

ALTER TABLE application_documents
    ADD COLUMN IF NOT EXISTS mime_type  VARCHAR(127),
    ADD COLUMN IF NOT EXISTS size_bytes BIGINT;
