package com.habitat.api.event;

import java.util.UUID;

/** Manager approved the viewing — tenant gets the confirmation push. */
public record ViewingApprovedEvent(UUID viewingId) {}
