package com.habitat.api.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Every outbound HTTP client is built with explicit timeouts. Backroom had a
 * RestTemplate with no timeouts and one bad Ozow call hung servlet threads
 * indefinitely — never again.
 *
 * For services that need circuit-breaking / retries, layer Resilience4j on
 * top via @CircuitBreaker / @Retry annotations.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}
