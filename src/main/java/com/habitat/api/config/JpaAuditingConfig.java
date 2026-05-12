package com.habitat.api.config;

import com.habitat.api.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * Drives BaseEntity's @CreatedBy / @LastModifiedBy. The auditor is the current
 * authenticated user, or empty for anonymous boot-time writes.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware(SecurityUtils security) {
        return () -> Optional.<UUID>ofNullable(security.currentUserId().orElse(null));
    }
}
