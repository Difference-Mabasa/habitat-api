package com.habitat.api.dto.mandate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /properties/{id}/mandate/reject}. The landlord
 * must supply a reason — the agent reads it and uses it to revise the
 * mandate (slice 4's request-changes/resubmit flow). 4-char minimum
 * stops trivially-empty rejections like "x" or "no"; 1000-char ceiling
 * keeps the column from being weaponised as a comment thread.
 */
public record RejectMandateRequest(
        @NotBlank
        @Size(min = 4, max = 1000)
        String reason
) {}
