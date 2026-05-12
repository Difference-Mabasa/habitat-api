-- Seed the four demo users that habitat-ui's DEMO_USERS map expects.
-- Per dev-standards §2, every demo user holds all four roles so the
-- workspaces dropdown is fully populated. activeRole differs per user
-- and drives which dashboard renders first.
--
-- Shared password (all 4 demo users): habitat123
-- bcrypt cost = 12 (matches the PasswordEncoder bean in SecurityConfig).
-- The hash below is one valid bcrypt encoding — every demo user shares
-- it so role-switching during development is one click.

WITH inserted_users AS (
    INSERT INTO users (id, email, password_hash, display_name, active_role, email_verified, area, created_at, updated_at)
    VALUES
        ('00000000-0000-0000-0000-000000000001', 'sipho@example.co.za',     '$2b$12$F46Qad/br/Mkds4Te8Lv3.1kjUb0eueKaSc/zToSihRfUFayjSliS', 'Sipho Dlamini',  'TENANT',   TRUE, 'Brixton',    NOW(), NOW()),
        ('00000000-0000-0000-0000-000000000002', 'thandi@example.co.za',    '$2b$12$F46Qad/br/Mkds4Te8Lv3.1kjUb0eueKaSc/zToSihRfUFayjSliS', 'Thandi Mokoena', 'LANDLORD', TRUE, 'Brixton',    NOW(), NOW()),
        ('00000000-0000-0000-0000-000000000003', 'naledi@vilakazi.co.za',   '$2b$12$F46Qad/br/Mkds4Te8Lv3.1kjUb0eueKaSc/zToSihRfUFayjSliS', 'Naledi M.',      'AGENT',    TRUE, 'Orlando West', NOW(), NOW()),
        ('00000000-0000-0000-0000-000000000004', 'trust@habitat.co.za',     '$2b$12$F46Qad/br/Mkds4Te8Lv3.1kjUb0eueKaSc/zToSihRfUFayjSliS', 'Habitat Trust',  'ADMIN',    TRUE, NULL,         NOW(), NOW())
    ON CONFLICT DO NOTHING
    RETURNING id
)
-- Every demo user gets the full role set (prototype convention until
-- real role assignment lands). The role grid is materialised across
-- both inserts and unioned by user_id.
INSERT INTO user_roles (user_id, role)
SELECT u.id, r.role
FROM (
    SELECT '00000000-0000-0000-0000-000000000001'::uuid AS id UNION ALL
    SELECT '00000000-0000-0000-0000-000000000002'::uuid UNION ALL
    SELECT '00000000-0000-0000-0000-000000000003'::uuid UNION ALL
    SELECT '00000000-0000-0000-0000-000000000004'::uuid
) u
CROSS JOIN (
    SELECT unnest(ARRAY['TENANT', 'LANDLORD', 'AGENT', 'ADMIN']) AS role
) r
ON CONFLICT DO NOTHING;
