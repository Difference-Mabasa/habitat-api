package com.habitat.api.service;

import com.habitat.api.constants.JwtConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Tracks revoked JWT jtis (access + refresh) in Redis. The
 * JwtAuthenticationFilter consults the same key prefix before letting a
 * request through.
 *
 * Redis is optional — when not wired (e.g. unit tests using a plain mock),
 * the service is a no-op. That keeps tests fast and the auth flow degrades
 * to "tokens are valid until expiry" rather than failing closed.
 */
@Service
public class TokenBlocklistService {

    private final StringRedisTemplate redis;

    @Autowired(required = false)
    public TokenBlocklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Constructor used by tests that want to drive the no-Redis branch. */
    public TokenBlocklistService() {
        this.redis = null;
    }

    /**
     * Mark a jti as revoked until {@code expiresAt}. If {@code expiresAt}
     * is already past, this is a no-op (the token can't be replayed anyway).
     */
    public void revoke(String jti, Instant expiresAt) {
        Objects.requireNonNull(jti, "jti");
        if (redis == null) return;
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) return;
        redis.opsForValue().set(key(jti), "1", ttl);
    }

    public boolean isRevoked(String jti) {
        if (redis == null || jti == null) return false;
        return Boolean.TRUE.equals(redis.hasKey(key(jti)));
    }

    private static String key(String jti) {
        return JwtConstants.BLOCKLIST_KEY_PREFIX + jti;
    }
}
