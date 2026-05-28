package com.habitat.api.dto.mandate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /properties/{id}/mandate/withdraw}. The agent
 * supplies a reason the landlord reads in the notification. Mirrors
 * the slice-3 {@link RejectMandateRequest} length bounds.
 */
public record WithdrawMandateRequest(
        @NotBlank
        @Size(min = 4, max = 1000)
        String reason
) {}
