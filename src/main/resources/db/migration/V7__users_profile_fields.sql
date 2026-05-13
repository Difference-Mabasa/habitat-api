-- Extend users with the profile-edit fields that previously lived only
-- on the (now-removed) onboarding wizard. Every column is nullable so
-- existing rows survive without a backfill.
--
-- Why these columns:
--   * phone               — SA mobile, for OTP + landlord contact.
--   * bio                 — short tenant intro shown on application review.
--   * interests           — small set of tags used by the recommender.
--   * job_title / employer / education
--                         — landlords lean on employment context.
--   * address_line ... longitude
--                         — current address. The structured form fields
--                           come from the Nominatim-backed AddressLookup
--                           component; lat/lng let us future-proof for
--                           "near me" searches and routing.

ALTER TABLE users
    ADD COLUMN phone         VARCHAR(20),
    ADD COLUMN bio           TEXT,
    ADD COLUMN interests     TEXT[],
    ADD COLUMN job_title     VARCHAR(120),
    ADD COLUMN employer      VARCHAR(120),
    ADD COLUMN education     VARCHAR(255),
    ADD COLUMN address_line  VARCHAR(255),
    ADD COLUMN suburb        VARCHAR(120),
    ADD COLUMN city          VARCHAR(120),
    ADD COLUMN province      VARCHAR(80),
    ADD COLUMN postal_code   VARCHAR(10),
    ADD COLUMN latitude      DOUBLE PRECISION,
    ADD COLUMN longitude     DOUBLE PRECISION;

-- Cheap-to-add indexes for the obvious search axes. We expect city +
-- suburb to anchor most "users near me" or "show me agents in Brixton"
-- type queries.
CREATE INDEX IF NOT EXISTS ix_users_city
    ON users (city) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_users_suburb
    ON users (suburb) WHERE deleted_at IS NULL;
