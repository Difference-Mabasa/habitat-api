package com.habitat.api.security;

import com.habitat.api.constants.JwtConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the {@code Authorization: Bearer ...} header, verifies the JWT, checks
 * the jti blocklist in Redis, and populates the SecurityContext.
 *
 * Skips itself when no header is present — anonymous requests go through and
 * SecurityConfig's matchers decide whether to allow them.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final StringRedisTemplate redis;

    public JwtAuthenticationFilter(JwtService jwt, @Autowired(required = false) StringRedisTemplate redis) {
        this.jwt = jwt;
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(JwtConstants.HEADER_AUTHORIZATION);
        if (header == null || !header.startsWith(JwtConstants.BEARER_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(JwtConstants.BEARER_PREFIX.length()).trim();
        try {
            HabitatPrincipal principal = jwt.verifyAccess(token);
            if (isBlocklisted(principal.jti())) {
                chain.doFilter(req, res);
                return;
            }

            var authorities = principal.roles().stream()
                    .map(r -> new SimpleGrantedAuthority(JwtConstants.ROLE_PREFIX + r.name()))
                    .toList();

            var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            MDC.put("userId", principal.userId().toString());
        } catch (RuntimeException ignored) {
            // Invalid token → unauthenticated request; downstream matchers handle it.
        }

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("userId");
        }
    }

    private boolean isBlocklisted(String jti) {
        if (redis == null || jti == null) return false;
        Boolean has = redis.hasKey(JwtConstants.BLOCKLIST_KEY_PREFIX + jti);
        return Boolean.TRUE.equals(has);
    }
}
