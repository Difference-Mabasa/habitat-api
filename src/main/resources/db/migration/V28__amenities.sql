-- V28: property amenities — the WiFi / Parking / Garden chips on the
-- wizard now persist against the property they were picked for.
--
-- Schema mirrors backroom-api's amenities domain (V1 there), with one
-- divergence:
--   - habitat tags amenities at the PROPERTY level (shared across all
--     units), matching the wizard's UI step. Backroom tagged per-unit;
--     that's a future Phase-12 refinement when habitat-ui grows a per-
--     unit amenity step.
--   - the icon column stores a Habitat outline-icon name (matches the
--     `IconName` union in habitat-ui/src/components/Icon.tsx), not
--     Material Design ligatures. The label is what backroom shows;
--     the icon name is Habitat-local.

CREATE TABLE IF NOT EXISTS amenities (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    icon        VARCHAR(40)  NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    -- BaseEntity columns
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS property_amenities (
    property_id UUID NOT NULL,
    amenity_id  UUID NOT NULL,
    PRIMARY KEY (property_id, amenity_id),
    CONSTRAINT property_amenities_property_fk
        FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    CONSTRAINT property_amenities_amenity_fk
        FOREIGN KEY (amenity_id) REFERENCES amenities(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS property_amenities_property_idx
    ON property_amenities (property_id);

-- Canonical 10. ids fixed so test fixtures can pin them and a future
-- referenced-by-name lookup is unnecessary.
INSERT INTO amenities (id, name, icon, sort_order, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000A0001'::uuid, 'WiFi',                 'wifi',    10, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A0002'::uuid, 'Parking',              'park',    20, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A0003'::uuid, 'Security gate',        'shield',  30, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A0004'::uuid, 'CCTV',                 'video',   40, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A0005'::uuid, 'Laundry',              'refresh', 50, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A0006'::uuid, 'Kitchen',              'flame',   60, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A0007'::uuid, 'Bathroom (en-suite)',  'bath',    70, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A0008'::uuid, 'Outdoor space',        'sun',     80, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A0009'::uuid, 'Storage',              'inbox',   90, NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000A000A'::uuid, 'Garden',               'sparkle', 100, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;
