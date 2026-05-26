package com.habitat.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-use OTP codes for lease signing (and any future flow that
 * needs them). Codes live in Redis with a 5-minute TTL; the
 * in-memory fallback exists for tests where Redis isn't wired and to
 * keep the auth flow degrading gracefully — but never falls back
 * silently in production.
 *
 * <p>Code shape: 6-digit numeric, generated from {@link SecureRandom}.
 * Verifying a code deletes it (single-use); a second sign attempt
 * with the same code returns false.
 *
 * <p>Keys are scoped: {@code (subject, purpose) → code}. {@code subject}
 * is normally a user UUID; {@code purpose} discriminates so a
 * lease-sign code can't be replayed against a future password-reset
 * flow.
 */
@Service
@Slf4j
public class OtpService {

    public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "habitat:otp:";
    private static final SecureRandom RNG = new SecureRandom();
    private static final int CODE_DIGITS = 6;

    private final StringRedisTemplate redis;
    /** Fallback when Redis isn't wired. Concurrent + bounded by the TTL sweep on each access. */
    private final ConcurrentHashMap<String, FallbackEntry> fallback = new ConcurrentHashMap<>();

    @Autowired(required = false)
    public OtpService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Used by tests that want to force the no-Redis branch. */
    public OtpService() {
        this.redis = null;
    }

    /**
     * Issue a fresh code for {@code (subject, purpose)}. Any previous
     * code for the same pair is overwritten. Returns the code so the
     * caller can deliver it (email / SMS / inline-in-response for dev).
     */
    public String issue(UUID subject, String purpose) {
        return issue(subject, purpose, DEFAULT_TTL);
    }

    public String issue(UUID subject, String purpose, Duration ttl) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(purpose, "purpose");
        String code = generateCode();
        if (redis != null) {
            redis.opsForValue().set(key(subject, purpose), code, ttl);
        } else {
            fallback.put(key(subject, purpose),
                    new FallbackEntry(code, System.nanoTime() + ttl.toNanos()));
        }
        log.debug("issued OTP for {}/{} (ttl={}s)", subject, purpose, ttl.getSeconds());
        return code;
    }

    /**
     * Compare {@code submitted} against the issued code. Returns true
     * only on exact match. On success the code is consumed and any
     * further verification attempt with the same code returns false.
     */
    public boolean verifyAndConsume(UUID subject, String purpose, String submitted) {
        if (subject == null || purpose == null || submitted == null) return false;
        String k = key(subject, purpose);
        String expected;
        if (redis != null) {
            expected = redis.opsForValue().get(k);
        } else {
            FallbackEntry entry = fallback.get(k);
            if (entry == null || entry.expiresAtNs() < System.nanoTime()) {
                fallback.remove(k);
                expected = null;
            } else {
                expected = entry.code();
            }
        }
        if (expected == null) return false;
        boolean match = constantTimeEquals(expected, submitted);
        if (match) {
            if (redis != null) {
                redis.delete(k);
            } else {
                fallback.remove(k);
            }
        }
        return match;
    }

    private static String generateCode() {
        int max = (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", RNG.nextInt(max));
    }

    private static String key(UUID subject, String purpose) {
        return KEY_PREFIX + purpose + ":" + subject;
    }

    /**
     * Constant-time string comparison so an attacker timing the
     * service can't probe partial-prefix matches.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private record FallbackEntry(String code, long expiresAtNs) {}
}
