-- V29: agent-managed mandates.
--
-- Ports the model from backroom-api PropertyMandate (V1 there). The
-- wizard's "Mandate" step on agent-managed listings now persists into
-- this table and the matching MandateController endpoints serve the
-- download / upload / email-to-landlord operations the UI exposes.
--
-- Status semantics mirror backroom:
--   PENDING_LANDLORD_APPROVAL  — online landlord (has a Habitat
--                                account); they tap "Approve" inside
--                                the app once they receive the
--                                notification.
--   PENDING_OFFLINE_SIGNATURE  — offline landlord; the wizard
--                                generates a PDF the agent emails /
--                                hands to the landlord. The landlord
--                                signs on paper; the agent re-uploads
--                                the signed PDF.
--   PENDING_AGENT_ACCEPTANCE   — landlord has signed; the agent
--                                attests that they hold the signed
--                                mandate.
--   ACTIVE                     — both sides accepted; the property
--                                publishes.
--   REJECTED / EXPIRED         — terminal states.
--
-- One mandate per property; LISTED listings owned by an agent must
-- reference an ACTIVE mandate. Habitat enforces that at the service
-- layer; the table doesn't UNIQUE on property_id because re-mandating
-- (after a REJECTED or EXPIRED) needs a fresh row.

CREATE TABLE IF NOT EXISTS mandates (
    id                       UUID         PRIMARY KEY,
    property_id              UUID         NOT NULL,
    agent_user_id            UUID         NOT NULL,
    -- online landlord: the user who owns the property on Habitat
    landlord_user_id         UUID,
    -- offline landlord (used when landlord_user_id is null)
    offline_landlord_name    VARCHAR(200),
    offline_landlord_email   VARCHAR(255),
    offline_landlord_phone   VARCHAR(50),
    mandate_type             VARCHAR(40)  NOT NULL,
    status                   VARCHAR(40)  NOT NULL,
    agent_attested           BOOLEAN      NOT NULL DEFAULT FALSE,
    fee_percent              NUMERIC(5,2),
    mandate_document_path    VARCHAR(500),
    signed_document_path     VARCHAR(500),
    notes                    TEXT,
    expires_at               TIMESTAMPTZ,
    -- BaseEntity columns
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT mandates_property_fk
        FOREIGN KEY (property_id)      REFERENCES properties(id) ON DELETE RESTRICT,
    CONSTRAINT mandates_agent_fk
        FOREIGN KEY (agent_user_id)    REFERENCES users(id)      ON DELETE RESTRICT,
    CONSTRAINT mandates_landlord_fk
        FOREIGN KEY (landlord_user_id) REFERENCES users(id)      ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS mandates_property_idx
    ON mandates (property_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS mandates_agent_idx
    ON mandates (agent_user_id) WHERE deleted_at IS NULL;
