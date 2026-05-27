-- V31: Backfill the new landlords table from existing properties +
-- mandates, then swap the properties.landlord_id FK from users →
-- landlords. After this runs:
--
--   LANDLORD_DIRECT properties   → ONLINE Landlord linked to the
--                                  current property owner (existing
--                                  user_id moves to landlord.user_id)
--   AGENT_MANAGED + online       → ONLINE Landlord linked to the
--                                  mandate's landlord_user_id
--   AGENT_MANAGED + offline      → OFFLINE Landlord, captured fields
--                                  copied from the mandate row
--   AGENT_MANAGED draft, no mandate → properties.landlord_id stays NULL
--                                  (the next mandate-issue sets it)

-- ── 1. Drop the old FK + relax the NOT NULL so the column can be
--      blanked while we swap referents from users → landlords.
ALTER TABLE properties DROP CONSTRAINT IF EXISTS fk_properties_landlord;
ALTER TABLE properties ALTER COLUMN landlord_id DROP NOT NULL;

-- ── 2. Stash legacy user_id so we can resolve it after we INSERT
--      landlord rows. NULL out the column itself so the new FK we
--      add later doesn't see stale user UUIDs as if they were
--      landlord UUIDs.
ALTER TABLE properties ADD COLUMN IF NOT EXISTS _migration_legacy_landlord_user_id UUID;
UPDATE properties SET _migration_legacy_landlord_user_id = landlord_id;
UPDATE properties SET landlord_id = NULL;

-- ── 3. ONLINE Landlords for LANDLORD_DIRECT properties ────────────
-- One row per unique owner. ON CONFLICT handles the shared-owner case
-- (same user owns multiple properties).
INSERT INTO landlords (id, type, user_id, created_at, updated_at, version)
SELECT DISTINCT
    gen_random_uuid(),
    'ONLINE',
    p._migration_legacy_landlord_user_id,
    NOW(),
    NOW(),
    0
FROM properties p
WHERE p.listing_mode = 'LANDLORD_DIRECT'
  AND p._migration_legacy_landlord_user_id IS NOT NULL
  AND p.deleted_at IS NULL
ON CONFLICT (user_id) WHERE user_id IS NOT NULL AND deleted_at IS NULL DO NOTHING;

-- ── 4. ONLINE Landlords for AGENT_MANAGED-with-online-landlord
INSERT INTO landlords (id, type, user_id, created_at, updated_at, version)
SELECT DISTINCT
    gen_random_uuid(),
    'ONLINE',
    m.landlord_user_id,
    NOW(),
    NOW(),
    0
FROM mandates m
WHERE m.landlord_user_id IS NOT NULL
  AND m.deleted_at IS NULL
ON CONFLICT (user_id) WHERE user_id IS NOT NULL AND deleted_at IS NULL DO NOTHING;

-- ── 5. OFFLINE Landlords for AGENT_MANAGED-with-offline-mandate
-- Temp column on landlords carries the source mandate id so step 6
-- can repoint properties via the mandate's property_id without an
-- ambiguous email-equality join.
ALTER TABLE landlords ADD COLUMN IF NOT EXISTS _migration_source_mandate_id UUID;

INSERT INTO landlords (
    id, type, created_by_agent_id, first_name, last_name, email, phone,
    _migration_source_mandate_id, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    'OFFLINE',
    m.agent_user_id,
    -- Split the captured "First Last" string on the first space.
    -- Single-word names land in first_name with last_name NULL —
    -- the agent can fix via the (Phase 2) edit flow.
    CASE
        WHEN POSITION(' ' IN m.offline_landlord_name) > 0
            THEN TRIM(SPLIT_PART(m.offline_landlord_name, ' ', 1))
        ELSE TRIM(m.offline_landlord_name)
    END,
    CASE
        WHEN POSITION(' ' IN m.offline_landlord_name) > 0
            THEN TRIM(SUBSTRING(m.offline_landlord_name FROM POSITION(' ' IN m.offline_landlord_name) + 1))
        ELSE NULL
    END,
    m.offline_landlord_email,
    m.offline_landlord_phone,
    m.id,
    NOW(),
    NOW(),
    0
FROM mandates m
WHERE m.landlord_user_id IS NULL
  AND m.offline_landlord_name IS NOT NULL
  AND m.deleted_at IS NULL;

-- ── 6. Repoint properties.landlord_id to the new landlord rows ────

-- LANDLORD_DIRECT: match via landlord.user_id = property's legacy user.
UPDATE properties p
SET landlord_id = l.id
FROM landlords l
WHERE p.listing_mode = 'LANDLORD_DIRECT'
  AND p._migration_legacy_landlord_user_id = l.user_id
  AND p.deleted_at IS NULL
  AND l.deleted_at IS NULL;

-- AGENT_MANAGED online: walk mandates → landlords by user_id.
UPDATE properties p
SET landlord_id = l.id
FROM mandates m
JOIN landlords l ON l.user_id = m.landlord_user_id
WHERE p.id = m.property_id
  AND m.landlord_user_id IS NOT NULL
  AND m.deleted_at IS NULL
  AND p.deleted_at IS NULL
  AND p.landlord_id IS NULL;

-- AGENT_MANAGED offline: walk via the temp column we wrote in step 5.
UPDATE properties p
SET landlord_id = l.id
FROM landlords l
JOIN mandates m ON m.id = l._migration_source_mandate_id
WHERE p.id = m.property_id
  AND p.deleted_at IS NULL
  AND p.landlord_id IS NULL;

-- ── 7. New FK pointing at landlords. Column was already relaxed
--      to nullable in step 1 so AGENT_MANAGED drafts with no
--      mandate (and any rows we couldn't resolve) stay unattributed.
ALTER TABLE properties
    ADD CONSTRAINT fk_properties_landlord
        FOREIGN KEY (landlord_id) REFERENCES landlords (id) ON DELETE RESTRICT;

-- ── 8. Drop the staging columns ──────────────────────────────────
ALTER TABLE landlords  DROP COLUMN IF EXISTS _migration_source_mandate_id;
ALTER TABLE properties DROP COLUMN IF EXISTS _migration_legacy_landlord_user_id;
