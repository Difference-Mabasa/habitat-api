-- Backfill welcome-notification CTAs after the /profile/onboarding route
-- was removed in favour of /profile (single edit surface that hydrates
-- from GET /users/me and autosaves per section).
--
-- Notification.action_url is captured at push-time and isn't recomputed
-- on read, so renaming the constant in ApiRoutes.UI_PROFILE_ONBOARDING
-- only affects notifications *issued after* the rename. Existing rows
-- still hold the stale URL — this UPDATE fixes them in place.
--
-- Scoped narrowly to WELCOME notifications so unrelated CTAs (none yet,
-- but any future SYSTEM / BILLING / etc. that legitimately point at a
-- sub-route of /profile) aren't accidentally rewritten.

UPDATE notifications
SET action_url = '/profile',
    updated_at = NOW()
WHERE type = 'WELCOME'
  AND action_url = '/profile/onboarding';
