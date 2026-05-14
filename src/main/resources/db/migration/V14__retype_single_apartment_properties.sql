-- V14: Retype six V10-seeded properties from APARTMENT_BLOCK → FLAT.
-- These rows each list exactly one APARTMENT-type unit, so the building-
-- level "block" label was misleading — the listing IS the apartment.
-- FLAT (added to PropertyType in this slice, mirrors backroom-api) is
-- the right shape: a single apartment as its own property.
--
-- Multi-unit blocks in V10 (hab-prop-2, hab-prop-4, hab-prop-11) stay
-- APARTMENT_BLOCK; V11/V12's bulk-seeded blocks (hab-prop-31..50) are
-- already real multi-unit buildings and need no retyping.
--
-- Idempotent: the UPDATE is constrained to property_type=APARTMENT_BLOCK
-- so re-running after manual edits is a no-op.

UPDATE properties
SET    property_type = 'FLAT',
       updated_at    = NOW()
WHERE  property_type = 'APARTMENT_BLOCK'
  AND  id IN (
    md5('hab-prop-5')::uuid,   -- Rosebank Loft Apartment
    md5('hab-prop-7')::uuid,   -- Sandton Central Studio Loft
    md5('hab-prop-9')::uuid,   -- Clifton Penthouse
    md5('hab-prop-10')::uuid,  -- Sea Point Compact Apartment
    md5('hab-prop-14')::uuid,  -- Umhlanga Ocean Apartment
    md5('hab-prop-15')::uuid   -- Pearls of Umhlanga Penthouse
  );
