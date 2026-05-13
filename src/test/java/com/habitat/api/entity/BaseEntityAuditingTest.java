package com.habitat.api.entity;

import com.habitat.api.enums.Role;
import com.habitat.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract that Spring Data auditing populates BaseEntity's
 * OffsetDateTime fields. The regression this guards against: Spring's
 * default DateTimeProvider returns LocalDateTime, which the bean wrapper
 * refuses to assign to OffsetDateTime. Without the custom provider in
 * JpaAuditingConfig every entity insert blows up with
 *
 *   IllegalArgumentException: Cannot convert unsupported date type
 *   java.time.LocalDateTime to java.time.OffsetDateTime
 *
 * which is what bit the auth flow at first run.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BaseEntityAuditingTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("habitat_test")
            .withUsername("habitat")
            .withPassword("habitat");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Autowired UserRepository users;

    @Test
    void persisting_an_entity_populates_offsetdatetime_audit_fields() {
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(2);

        User saved = users.saveAndFlush(User.builder()
                .email("audit-" + System.nanoTime() + "@habitat.test")
                .passwordHash("not-a-real-hash")
                .firstName("Audit")
                .surname("Probe")
                .roles(Set.of(Role.TENANT))
                .activeRole(Role.TENANT)
                .build());

        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(2);

        // Both audit fields must be set. The fix in JpaAuditingConfig wires a
        // DateTimeProvider that returns OffsetDateTime in UTC; if the default
        // (LocalDateTime) provider is ever reinstated, the persist itself fails.
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        // Truncate to seconds before comparing — Postgres timestamp(6) round-trips
        // microseconds, but JVM/DB clock skew can still drift sub-millisecond.
        assertThat(saved.getCreatedAt())
                .isBetween(before.truncatedTo(ChronoUnit.SECONDS), after);
        assertThat(saved.getUpdatedAt())
                .isBetween(before.truncatedTo(ChronoUnit.SECONDS), after);

        // The provider returns UTC explicitly so audit reads are predictable
        // regardless of the host JVM's default zone.
        assertThat(saved.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(saved.getUpdatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}
