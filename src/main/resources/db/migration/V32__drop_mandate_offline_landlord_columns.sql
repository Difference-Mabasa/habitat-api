-- V32: Owner identity now lives on landlords (via property.landlord_id);
-- mandate rows no longer carry their own copy. V31 already backfilled
-- the captured offline fields into landlords rows.

ALTER TABLE mandates DROP CONSTRAINT IF EXISTS mandates_landlord_fk;
DROP INDEX IF EXISTS mandates_landlord_idx;

ALTER TABLE mandates DROP COLUMN IF EXISTS landlord_user_id;
ALTER TABLE mandates DROP COLUMN IF EXISTS offline_landlord_name;
ALTER TABLE mandates DROP COLUMN IF EXISTS offline_landlord_email;
ALTER TABLE mandates DROP COLUMN IF EXISTS offline_landlord_phone;
