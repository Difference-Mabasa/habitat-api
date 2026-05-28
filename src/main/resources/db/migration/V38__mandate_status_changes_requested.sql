-- Slice 4: extend MandateStatus enum with CHANGES_REQUESTED.
-- mandates.status is VARCHAR(40) (V29), so the constraint lives in
-- com.habitat.api.enums.MandateStatus, not the DB. This file exists
-- as the audit marker per dev-standards §9: every new enum value gets
-- a migration file even when there's no schema change.
SELECT 1;
