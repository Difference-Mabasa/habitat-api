package com.habitat.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test — the Spring context loads against a real PostgreSQL and
 * Flyway runs every migration cleanly.
 *
 * If this test goes red, the core scaffold is broken.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class HabitatApiApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("habitat_test")
            .withUsername("habitat")
            .withPassword("habitat");

    @DynamicPropertySource
    static void postgresProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Redis is wired but the smoke test doesn't need it — disable cache.
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Test
    void contextLoads() {
        // Pass = context boots, Flyway runs every migration.
    }
}
