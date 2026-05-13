-- Split users.display_name into first_name + surname.
--
-- Why: registration now collects a first name and a surname separately;
-- composing a single display_name client-side is the wrong model for
-- a system that wants to greet, sort, and address people by first name.
--
-- Backfill strategy for the V2 seed (Sipho Dlamini, Thandi Mokoena,
-- Naledi M., Habitat Trust):
--   * everything before the first space  -> first_name
--   * everything after the first space   -> surname
--   * when the row has no space, surname falls back to '—' so the
--     NOT NULL constraint holds (Habitat Trust is the only edge case
--     here, and the trim() / nullif() pair handles it).
--
-- After backfill, both columns are NOT NULL and display_name is dropped —
-- the entity-level getDisplayName() composes from first_name + surname.

ALTER TABLE users
    ADD COLUMN first_name VARCHAR(40),
    ADD COLUMN surname    VARCHAR(40);

UPDATE users
SET first_name = CASE
                     WHEN position(' ' IN display_name) > 0
                         THEN substring(display_name FROM 1 FOR position(' ' IN display_name) - 1)
                     ELSE display_name
                 END,
    surname = CASE
                  WHEN position(' ' IN display_name) > 0
                      THEN nullif(trim(substring(display_name FROM position(' ' IN display_name) + 1)), '')
                  ELSE NULL
              END;

-- Anyone without a recoverable surname gets '—' so the NOT NULL holds.
UPDATE users SET surname = '—' WHERE surname IS NULL;

ALTER TABLE users
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN surname    SET NOT NULL;

ALTER TABLE users DROP COLUMN display_name;
