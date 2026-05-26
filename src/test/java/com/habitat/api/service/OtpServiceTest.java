package com.habitat.api.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the no-Redis branch of {@link OtpService}. The Redis-backed
 * branch shares the same logic — keys and TTLs are pushed straight at
 * {@link org.springframework.data.redis.core.StringRedisTemplate} so
 * an integration test against a real Redis is the cheapest way to
 * cover that path; not added here to keep the unit suite Redis-free.
 */
class OtpServiceTest {

    private final OtpService otp = new OtpService();

    @Test
    void issued_code_is_a_six_digit_string() {
        String code = otp.issue(UUID.randomUUID(), "lease-sign");
        assertThat(code).hasSize(6).matches("\\d{6}");
    }

    @Test
    void verify_and_consume_succeeds_with_the_issued_code() {
        UUID subject = UUID.randomUUID();
        String code = otp.issue(subject, "lease-sign");
        assertThat(otp.verifyAndConsume(subject, "lease-sign", code)).isTrue();
    }

    @Test
    void verify_and_consume_fails_for_a_wrong_code() {
        UUID subject = UUID.randomUUID();
        otp.issue(subject, "lease-sign");
        assertThat(otp.verifyAndConsume(subject, "lease-sign", "000000")).isFalse();
    }

    @Test
    void code_is_single_use() {
        UUID subject = UUID.randomUUID();
        String code = otp.issue(subject, "lease-sign");
        assertThat(otp.verifyAndConsume(subject, "lease-sign", code)).isTrue();
        assertThat(otp.verifyAndConsume(subject, "lease-sign", code)).isFalse();
    }

    @Test
    void purpose_is_scoped_so_a_code_does_not_leak_across_flows() {
        UUID subject = UUID.randomUUID();
        String code = otp.issue(subject, "lease-sign");
        assertThat(otp.verifyAndConsume(subject, "password-reset", code)).isFalse();
        // Original purpose still verifies — the failed cross-purpose
        // attempt does not consume the lease-sign code.
        assertThat(otp.verifyAndConsume(subject, "lease-sign", code)).isTrue();
    }

    @Test
    void issuing_a_new_code_overwrites_the_previous_one() {
        UUID subject = UUID.randomUUID();
        String first = otp.issue(subject, "lease-sign");
        String second = otp.issue(subject, "lease-sign");
        // Even if (very rarely) the two RNG calls collide, the second
        // call still overwrites the first entry — the verify on the
        // first code returns true *exactly when* the codes happen to
        // match. Assert behaviour at the API level rather than equality
        // of the two strings.
        assertThat(otp.verifyAndConsume(subject, "lease-sign", second)).isTrue();
        if (!first.equals(second)) {
            assertThat(otp.verifyAndConsume(subject, "lease-sign", first)).isFalse();
        }
    }

    @Test
    void code_expires_after_the_ttl() throws Exception {
        UUID subject = UUID.randomUUID();
        String code = otp.issue(subject, "lease-sign", Duration.ofMillis(50));
        Thread.sleep(120);
        assertThat(otp.verifyAndConsume(subject, "lease-sign", code)).isFalse();
    }

    @Test
    void null_inputs_return_false_without_throwing() {
        assertThat(otp.verifyAndConsume(null, "lease-sign", "123456")).isFalse();
        assertThat(otp.verifyAndConsume(UUID.randomUUID(), null, "123456")).isFalse();
        assertThat(otp.verifyAndConsume(UUID.randomUUID(), "lease-sign", null)).isFalse();
    }
}
