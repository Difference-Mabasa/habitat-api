package com.habitat.api.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Outbound HTTP client. We use {@link RestClient} (Spring 6.1+) rather than
 * the legacy {@code RestTemplate} or the third-party {@code FeignClient}:
 *
 *  - <b>vs RestTemplate:</b> RestClient is the modern fluent successor; Spring
 *    docs explicitly steer new code to it. Same call-site clarity, same
 *    timeout knobs.
 *  - <b>vs Feign / Spring Cloud OpenFeign:</b> we don't want the Spring Cloud
 *    dependency tree, and for the small number of outbound providers we
 *    integrate with (payment, email, S3) the interface-per-call ceremony
 *    is over-engineering today.
 *  - <b>Declarative when we need it:</b> when we have multiple internal
 *    services or want a Feign-like surface, wrap calls in
 *    {@code @HttpExchange} interfaces backed by this same RestClient.
 *    See {@code development-standards.md} §9.
 *
 * Every outbound HTTP call carries explicit timeouts — connect 3s, read 10s.
 * For resilience (circuit breakers, retries) layer Resilience4j annotations
 * on the calling service method, never on the bean.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(10));
        return builder
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
