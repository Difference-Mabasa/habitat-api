-- V23 (Tier B): drop the destructive CASCADE on notifications.recipient_id.
--
-- V5 explicitly added read_at "to preserve the 'when was this dismissed'
-- audit signal without losing it to TTL pruning" — but the same migration
-- attached ON DELETE CASCADE to recipient_id, which destroys the entire
-- audit trail the moment the user is hard-deleted. The two design choices
-- contradict each other. Resolve in favour of the audit intent.
--
-- After V23:
--   notifications.recipient_id NOT NULL ON DELETE RESTRICT
--
-- Why RESTRICT (not SET NULL):
--   * Users in habitat are soft-deleted via deleted_at — the @SQLRestriction
--     filter hides them, but the row stays. So the cascade was only ever a
--     trap for *hard* deletes (admin cleanup, GDPR/POPIA right-to-erasure).
--   * Hard-deleting a user with notifications is intentional and rare.
--     Forcing the admin to handle the notifications explicitly (anonymise,
--     archive, or delete) is the right safety net.
--   * SET NULL would lose the recipient identity and require making
--     recipient_id nullable — a bigger entity / service change for a path
--     that should be exceptional. Revisit if POPIA right-to-erasure ever
--     becomes a hot path.

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS fk_notifications_recipient;
ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE RESTRICT;
