package com.habitat.api.entity;

import com.habitat.api.enums.Role;
import com.habitat.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves BUG-01 (TECH_DEBT.md) is closed: BaseEntity's {@code @Version}
 * column makes Hibernate detect stale writes and surface them as
 * {@link ObjectOptimisticLockingFailureException}, which
 * {@link com.habitat.api.exception.GlobalExceptionHandler} maps to
 * 409 STALE_RESOURCE.
 *
 * The race we're guarding: two reviewers (or two double-clicks) load
 * the same row at version N, both write — without {@code @Version} the
 * last writer silently wins. With it, the second write throws.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OptimisticLockingTest {

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
    void version_increments_on_each_write() {
        User saved = users.saveAndFlush(User.builder()
                .email("ver-" + System.nanoTime() + "@habitat.test")
                .passwordHash("not-a-real-hash")
                .firstName("Ver")
                .surname("Probe")
                .roles(Set.of(Role.USER))
                .activeRole(Role.USER)
                .build());

        assertThat(saved.getVersion()).isZero();

        saved.setFirstName("Ver2");
        User updated = users.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void stale_write_throws_optimistic_lock_exception() {
        User saved = users.saveAndFlush(User.builder()
                .email("stale-" + System.nanoTime() + "@habitat.test")
                .passwordHash("not-a-real-hash")
                .firstName("Stale")
                .surname("Probe")
                .roles(Set.of(Role.USER))
                .activeRole(Role.USER)
                .build());

        // First write — version 0 → 1 in DB.
        saved.setFirstName("First");
        users.saveAndFlush(saved);

        // Simulate a stale read: reset the in-memory version to 0 so the
        // next save carries an outdated version. Hibernate compares the
        // entity's version to the row's and throws on mismatch.
        ReflectionTestUtils.setField(saved, "version", 0L);
        saved.setFirstName("Second");

        assertThatThrownBy(() -> users.saveAndFlush(saved))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
