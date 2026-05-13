-- Property + Unit + photo tables.
--
-- Carries the backroom-api domain shape (rental_properties + units +
-- property_images + unit_images) with these adjustments:
--   * Renamed properties (vs rental_properties) — there's no non-rental
--     concept in habitat to disambiguate from.
--   * Soft-delete columns from BaseEntity present on every table — backroom
--     hard-deleted images, lost audit trail on disputes.
--   * Indexes on (status, suburb, city, unit_type, price, bedrooms) since
--     every public-listings query filters or sorts on these.

-- ── Properties ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS properties (
    id              UUID         PRIMARY KEY,
    landlord_id     UUID         NOT NULL,
    manager_id      UUID         NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    property_type   VARCHAR(40)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    address_line    VARCHAR(255),
    suburb          VARCHAR(120),
    city            VARCHAR(120),
    province        VARCHAR(80),
    postal_code     VARCHAR(10),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      UUID,
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT fk_properties_landlord FOREIGN KEY (landlord_id) REFERENCES users (id),
    CONSTRAINT fk_properties_manager  FOREIGN KEY (manager_id)  REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS ix_properties_status
    ON properties (status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_properties_manager
    ON properties (manager_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_properties_suburb
    ON properties (LOWER(suburb)) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_properties_city
    ON properties (LOWER(city)) WHERE deleted_at IS NULL;

-- ── Units ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS units (
    id                    UUID           PRIMARY KEY,
    property_id           UUID           NOT NULL,
    unit_number           VARCHAR(40),
    title                 VARCHAR(200)   NOT NULL,
    description           TEXT,
    unit_type             VARCHAR(40)    NOT NULL,
    status                VARCHAR(32)    NOT NULL DEFAULT 'AVAILABLE',
    furnishing            VARCHAR(20),
    price                 NUMERIC(12, 2) NOT NULL,
    payment_frequency     VARCHAR(16)    NOT NULL DEFAULT 'MONTHLY',
    deposit               NUMERIC(12, 2),
    bedrooms              INTEGER        NOT NULL,
    bathrooms             INTEGER        NOT NULL,
    sqm                   INTEGER,
    max_occupants         INTEGER,
    water_included        BOOLEAN        NOT NULL DEFAULT FALSE,
    electricity_included  BOOLEAN        NOT NULL DEFAULT FALSE,
    pets_allowed          BOOLEAN        NOT NULL DEFAULT FALSE,
    available_from        DATE,
    created_at            TIMESTAMPTZ    NOT NULL,
    updated_at            TIMESTAMPTZ    NOT NULL,
    created_by            UUID,
    updated_by            UUID,
    deleted_at            TIMESTAMPTZ,
    CONSTRAINT fk_units_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_units_property
    ON units (property_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_units_status_type
    ON units (status, unit_type) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_units_price
    ON units (price) WHERE deleted_at IS NULL AND status = 'AVAILABLE';
CREATE INDEX IF NOT EXISTS ix_units_bedrooms
    ON units (bedrooms) WHERE deleted_at IS NULL AND status = 'AVAILABLE';

-- ── Property images ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS property_images (
    id           UUID         PRIMARY KEY,
    property_id  UUID         NOT NULL,
    url          VARCHAR(500) NOT NULL,
    is_cover     BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    created_by   UUID,
    updated_by   UUID,
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_property_images_property
    ON property_images (property_id, sort_order) WHERE deleted_at IS NULL;

-- ── Unit images ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS unit_images (
    id          UUID         PRIMARY KEY,
    unit_id     UUID         NOT NULL,
    url         VARCHAR(500) NOT NULL,
    is_cover    BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT fk_unit_images_unit FOREIGN KEY (unit_id) REFERENCES units (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_unit_images_unit
    ON unit_images (unit_id, sort_order) WHERE deleted_at IS NULL;
