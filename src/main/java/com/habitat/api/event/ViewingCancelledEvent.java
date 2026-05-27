package com.habitat.api.event;

import java.util.UUID;

/**
 * Either party cancelled the viewing. Listener determines who the
 * OTHER party is via {@code viewing.cancelledBy} (the actor) and
 * pushes the cancellation notice only to them — the actor sees a
 * toast, not a notification of their own action.
 */
public record ViewingCancelledEvent(UUID viewingId) {}
