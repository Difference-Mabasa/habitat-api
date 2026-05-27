package com.habitat.api.dto.viewing;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tenant payload for {@code POST /viewings}. {@code scheduledAt} must
 * be in the future at the time the API receives it — the @Future
 * validator catches a stale slot before we hit the service.
 */
public record RequestViewingRequest(
        @NotNull UUID unitId,
        @NotNull @Future OffsetDateTime scheduledAt,
        @Size(max = 1000) String notes
) {}
