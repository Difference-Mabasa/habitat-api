package com.habitat.api.dto.lease;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /leases/{id}/sign}. Phase 6: OTP is required
 * and verified server-side against the most-recent code issued by
 * {@code POST /leases/{id}/otp}. The code is single-use — a second
 * sign attempt with the same code returns LEASE_OTP_INVALID.
 */
public record SignLeaseRequest(
        @NotBlank @Size(min = 4, max = 12) String otp
) {}
