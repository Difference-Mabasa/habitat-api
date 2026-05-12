package com.habitat.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlocklistServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    TokenBlocklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlocklistService(redis);
    }

    @Test
    void revoke_writes_to_redis_with_ttl_equal_to_remaining_lifetime() {
        when(redis.opsForValue()).thenReturn(valueOps);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));

        service.revoke("jti-1", expiresAt);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(any(String.class), anyString(), ttl.capture());
        // Allow a couple of seconds of slack since Instant.now() runs inside revoke().
        assertThat(ttl.getValue()).isBetween(Duration.ofMinutes(14).plusSeconds(50), Duration.ofMinutes(15));
    }

    @Test
    void revoke_is_a_noop_when_token_already_expired() {
        service.revoke("jti-x", Instant.now().minusSeconds(1));
        verifyNoInteractions(redis);
    }

    @Test
    void revoke_is_a_noop_when_redis_unavailable() {
        TokenBlocklistService noRedis = new TokenBlocklistService();
        // No Redis bean wired — should not throw.
        noRedis.revoke("jti", Instant.now().plusSeconds(60));
        // No exception means success — there's nothing else to assert.
    }

    @Test
    void revoke_rejects_null_jti() {
        assertThatNullPointerException()
                .isThrownBy(() -> service.revoke(null, Instant.now().plusSeconds(60)));
    }

    @Test
    void isRevoked_returns_true_when_redis_has_key() {
        when(redis.hasKey("jwt:blocklist:abc")).thenReturn(true);
        assertThat(service.isRevoked("abc")).isTrue();
    }

    @Test
    void isRevoked_returns_false_when_redis_has_no_key() {
        when(redis.hasKey("jwt:blocklist:abc")).thenReturn(false);
        assertThat(service.isRevoked("abc")).isFalse();
    }

    @Test
    void isRevoked_returns_false_for_null_jti() {
        assertThat(service.isRevoked(null)).isFalse();
        verify(redis, never()).hasKey(any(String.class));
    }

    @Test
    void isRevoked_returns_false_when_redis_unavailable() {
        TokenBlocklistService noRedis = new TokenBlocklistService();
        assertThat(noRedis.isRevoked("anything")).isFalse();
    }
}
