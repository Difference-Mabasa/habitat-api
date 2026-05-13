package com.habitat.api.dto.notification;

/**
 * Tiny payload for the bell-icon badge. Kept separate from the paginated
 * list endpoint so the badge can poll cheaply without dragging the latest
 * notification bodies over the wire on every tick.
 */
public record UnreadCountResponse(long count) {}
