-- V13: Aggregated rating storage on properties so the landing's
-- "Top Rated Near You" section has something to rank by. Mirrors
-- backroom-api's RentalProperty.avg_rating / rating_count pattern;
-- the values are recomputed from reviews (deferred) — for now every
-- existing seeded row gets the DEFAULT 0 and the top-rated endpoint
-- falls back to createdAt-DESC ordering until reviews land.

ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS avg_rating   NUMERIC(3,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rating_count INTEGER      NOT NULL DEFAULT 0;

-- Index supports the ORDER BY avg_rating DESC, rating_count DESC pattern
-- used by /properties/top-rated. Status filter is the actual hot path
-- (LISTED only), so a partial index keeps it small.
CREATE INDEX IF NOT EXISTS idx_properties_rating
    ON properties (avg_rating DESC, rating_count DESC)
    WHERE status = 'LISTED';
