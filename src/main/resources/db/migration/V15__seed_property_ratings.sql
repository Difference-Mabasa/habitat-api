-- V15: Populate avg_rating + rating_count on the V10/V11/V12 seeded
-- properties so the landing "Top Rated Near You" carousel actually shows
-- a star distribution instead of every card reading "New". Until a real
-- review-submission flow lands, this is the only way the demo catalogue
-- gets non-zero ratings.
--
-- Determinism: ratings are derived from hashtext(id::text), so re-running
-- this migration would yield the same values (idempotent in spirit, even
-- though Flyway versioning already guarantees it only runs once).
--
-- Distribution:
--   - ~10% of properties stay at avg_rating=0, rating_count=0
--     → keeps the "New" tag visible for demo
--   - The other ~90%:
--       avg_rating   ∈ [3.50, 4.99] (NUMERIC(3,2)) — capped at 5★ by column scale
--       rating_count ∈ [1, 80]
--
-- Only touches rows that are still at default zero so a hypothetical
-- real review system writing ratings won't be clobbered if migrations
-- replay.

UPDATE properties p
SET    avg_rating   = sub.new_avg,
       rating_count = sub.new_count,
       updated_at   = NOW()
FROM (
    SELECT
        id,
        CASE
            WHEN (abs(hashtext(id::text)) % 10) = 0 THEN 0::numeric(3,2)
            ELSE (3.50 + (abs(hashtext(id::text)) % 150) / 100.0)::numeric(3,2)
        END AS new_avg,
        CASE
            WHEN (abs(hashtext(id::text)) % 10) = 0 THEN 0
            ELSE (abs(hashtext(id::text || 'c')) % 80) + 1
        END AS new_count
    FROM properties
) sub
WHERE  p.id = sub.id
  AND  p.avg_rating = 0
  AND  p.rating_count = 0;
