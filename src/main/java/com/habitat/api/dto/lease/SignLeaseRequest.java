package com.habitat.api.dto.lease;

import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /leases/{id}/sign}. OTP signing is mocked
 * today — the caller passes a 6-digit OTP that the service trivially
 * accepts. Real e-IDAS qualified signing lands in a later slice.
 */
public record SignLeaseRequest(
        @Size(min = 4, max = 12) String otp
) {}
