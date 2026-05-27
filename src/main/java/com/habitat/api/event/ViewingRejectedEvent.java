package com.habitat.api.event;

import java.util.UUID;

/** Manager declined the viewing — tenant gets the rejection push. */
public record ViewingRejectedEvent(UUID viewingId) {}
