-- "At most one non-terminal mandate per property" — enforced at the
-- DB level, not just at the service. A mandate is the landlord's
-- attestation that the agent is authorised to publish the property,
-- so concurrent in-flight ones don't make sense. Terminal rows
-- (REJECTED, EXPIRED) stay around as history; new rows can be issued
-- on top of them.
--
-- Pre-V41 the V29 docs said "Habitat enforces that at the service
-- layer; the table doesn't UNIQUE on property_id because re-mandating
-- (after a REJECTED or EXPIRED) needs a fresh row" — this migration
-- moves that to a partial unique index so the DB is the source of
-- truth.

-- Defensive backfill: if any property somehow has more than one
-- non-terminal mandate, mark the older ones REJECTED with an
-- auto-archive reason so the partial unique index can be created.
-- The "row_number partition by property" picks the most-recent
-- in-flight row per property and demotes the others.
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY property_id
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM mandates
    WHERE deleted_at IS NULL
      AND status NOT IN ('REJECTED', 'EXPIRED')
)
UPDATE mandates m
   SET status            = 'REJECTED',
       rejection_reason  = 'Superseded — auto-archived during the V41 one-mandate-per-property migration.',
       rejected_at       = NOW(),
       updated_at        = NOW()
  FROM ranked r
 WHERE m.id = r.id
   AND r.rn > 1;

-- The constraint itself: one non-soft-deleted, non-terminal mandate
-- per property. REJECTED + EXPIRED rows can pile up as history; one
-- non-terminal at a time. Hibernate's SQLRestriction("deleted_at IS NULL")
-- already keeps soft-deleted rows out of the app, but the index
-- includes the same predicate so it's enforceable from raw SQL too.
CREATE UNIQUE INDEX IF NOT EXISTS mandates_one_non_terminal_per_property_idx
    ON mandates (property_id)
 WHERE deleted_at IS NULL
   AND status NOT IN ('REJECTED', 'EXPIRED');
