package com.habitat.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Stamps every request with a correlation id. Available in:
 *  - MDC as {@code requestId} (so every log line includes it)
 *  - the {@code X-Request-Id} response header
 *  - {@link com.habitat.api.dto.ApiError#requestId()} on every error response
 *
 * If the incoming request supplies an {@code X-Request-Id} we honour it
 * (lets the frontend trace its own retries). Otherwise we generate one.
 */
@Component
@Order(0)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String incoming = req.getHeader(HEADER);
        String requestId = (incoming != null && !incoming.isBlank())
                ? incoming.trim()
                : UUID.randomUUID().toString();
        try {
            MDC.put(MDC_KEY, requestId);
            res.setHeader(HEADER, requestId);
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
