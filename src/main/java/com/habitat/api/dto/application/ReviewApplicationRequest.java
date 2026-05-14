package com.habitat.api.dto.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code PATCH /applications/{id}/review} — landlord's verdict
 * on a submitted application. The {@code action} picks the target
 * status; {@code note} is the free-text rationale that flows back to
 * the tenant.
 */
public record ReviewApplicationRequest(
        @NotNull Action action,

        @Size(max = 2000)
        String note
) {
    public enum Action {
        APPROVE,
        REJECT,
        ON_HOLD
    }
}
