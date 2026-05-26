package com.habitat.api.dto.lease;

/**
 * Response of {@code POST /leases/{id}/otp}.
 *
 * <p>{@code devCode} is non-null in environments where email delivery
 * isn't wired (everything pre-Phase 8). It lets the UI surface the
 * code inline so a tenant testing the flow doesn't need an inbox.
 * Production builds set {@code devCode} to {@code null} once Resend
 * is the canonical delivery channel.
 */
public record IssueOtpResponse(String devCode) {}
