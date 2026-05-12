package com.habitat.api.config;

import com.habitat.api.constants.PublicEndpoints;
import com.habitat.api.security.JwtAuthenticationFilter;
import com.habitat.api.security.RequestIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Security baseline shipped on day one — none of the "we'll fix it before
 * prod" headers / settings backroom had to backfill.
 *
 *  - HSTS, X-Frame-Options DENY, nosniff, CSP, Referrer-Policy by default.
 *  - Stateless: no session, no cookies.
 *  - All non-public endpoints require auth; PublicEndpoints is the only
 *    list of allowed-anonymous paths.
 *  - RequestIdFilter runs first so correlation IDs appear in every log line.
 *  - JwtAuthenticationFilter runs before the Spring username/password filter.
 *  - CORS allowedHeaders is a finite whitelist, never "*".
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.origins}")
    private String corsOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList(corsOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Requested-With", "X-Request-Id"
        ));
        cfg.setExposedHeaders(List.of("X-Request-Id"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            RequestIdFilter requestIdFilter,
            JwtAuthenticationFilter jwtFilter) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000))
                    .contentTypeOptions(Customizer.withDefaults())
                    .frameOptions(FrameOptionsConfig::deny)
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                    .referrerPolicy(rp -> rp.policy(
                            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .authorizeHttpRequests(reg -> {
                for (String path : PublicEndpoints.PATHS) {
                    reg.requestMatchers(path).permitAll();
                }
                reg.requestMatchers("/actuator/**").hasRole("ADMIN");
                reg.anyRequest().authenticated();
            })
            .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
