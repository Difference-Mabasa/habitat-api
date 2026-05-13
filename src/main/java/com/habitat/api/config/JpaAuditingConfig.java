package com.habitat.api.config;

import com.habitat.api.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Drives BaseEntity's @CreatedBy / @LastModifiedBy / @CreatedDate / @LastModifiedDate.
 *
 * The auditor is the current authenticated user, or empty for anonymous
 * boot-time writes.
 *
 * Spring's default DateTimeProvider returns LocalDateTime, which Spring
 * Data's bean wrapper cannot convert to OffsetDateTime — BaseEntity uses
 * OffsetDateTime throughout, so we provide UTC OffsetDateTime explicitly.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware(SecurityUtils security) {
        return () -> Optional.<UUID>ofNullable(security.currentUserId().orElse(null));
    }

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
