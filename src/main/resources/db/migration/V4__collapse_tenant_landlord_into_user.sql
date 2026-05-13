-- Collapse the TENANT and LANDLORD roles into a single USER role.
--
-- Rationale: "tenant" and "landlord" are *states*, not auth roles —
-- someone is a tenant if they hold a lease, a landlord if they're the
-- manager of a property. The auth identity is just USER. This aligns
-- habitat with backroom-api's "R-1 Role & Ownership Rewrite" (their
-- V35 migration), where UserRole.TENANT and UserRole.MANAGER were
-- deprecated and all rows backfilled to UserRole.USER.
--
-- Both role columns are VARCHAR with @Enumerated(EnumType.STRING) on
-- the entity side, so this is a plain string remap.

-- users.active_role only has one value per row, so a straight UPDATE is safe.
UPDATE users
SET active_role = 'USER'
WHERE active_role IN ('TENANT', 'LANDLORD');

-- user_roles is a join table with PRIMARY KEY (user_id, role). A single user
-- often holds *both* TENANT and LANDLORD — a naive UPDATE-to-'USER' would
-- collide on the unique constraint mid-statement. Insert the new USER row
-- first (idempotent via ON CONFLICT), then drop the old ones.
INSERT INTO user_roles (user_id, role)
SELECT DISTINCT user_id, 'USER'
FROM user_roles
WHERE role IN ('TENANT', 'LANDLORD')
ON CONFLICT DO NOTHING;

DELETE FROM user_roles
WHERE role IN ('TENANT', 'LANDLORD');
