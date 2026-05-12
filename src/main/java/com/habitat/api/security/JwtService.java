package com.habitat.api.security;

import com.habitat.api.constants.ErrorMessages;
import com.habitat.api.constants.JwtConstants;
import com.habitat.api.entity.User;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * JWT issue + verify. Lessons from backroom:
 *  - JWT secret is validated at startup (32+ bytes) so a weak dev default
 *    can never accidentally ship to prod.
 *  - Every token carries a {@code jti} so we can revoke individual tokens
 *    via a Redis blocklist (wired in JwtAuthenticationFilter).
 *  - The token "kind" claim distinguishes access vs refresh — they can't
 *    be swapped.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl}") Duration accessTtl,
            @Value("${app.jwt.refresh-ttl}") Duration refreshTtl) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    @PostConstruct
    void validateSecretLength() {
        // Belt-and-braces — the Keys.hmacShaKeyFor() above already throws on weak keys,
        // but this gives a clearer error message during boot.
        // 32 bytes = 256 bits = minimum for HS256.
        // We re-derive byte length from the key.
        if (signingKey.getEncoded().length < 32) {
            throw new IllegalStateException(ErrorMessages.JWT_SECRET_TOO_SHORT);
        }
    }

    public IssuedToken issueAccess(User user) {
        return issue(user, JwtConstants.TOKEN_KIND_ACCESS, accessTtl);
    }

    public IssuedToken issueRefresh(User user) {
        return issue(user, JwtConstants.TOKEN_KIND_REFRESH, refreshTtl);
    }

    public HabitatPrincipal verifyAccess(String token) {
        Claims claims = parse(token);
        if (!JwtConstants.TOKEN_KIND_ACCESS.equals(claims.get(JwtConstants.CLAIM_TOKEN_KIND, String.class))) {
            throw new UnauthorizedException(ErrorMessages.JWT_INVALID);
        }
        return toPrincipal(claims);
    }

    public HabitatPrincipal verifyRefresh(String token) {
        Claims claims = parse(token);
        if (!JwtConstants.TOKEN_KIND_REFRESH.equals(claims.get(JwtConstants.CLAIM_TOKEN_KIND, String.class))) {
            throw new UnauthorizedException(ErrorMessages.JWT_INVALID);
        }
        return toPrincipal(claims);
    }

    private IssuedToken issue(User user, String kind, Duration ttl) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .claim(JwtConstants.CLAIM_USER_ID, user.getId().toString())
                .claim(JwtConstants.CLAIM_EMAIL, user.getEmail())
                .claim(JwtConstants.CLAIM_ROLES,
                        user.getRoles().stream().map(Enum::name).toList())
                .claim(JwtConstants.CLAIM_ACTIVE_ROLE, user.getActiveRole().name())
                .claim(JwtConstants.CLAIM_TOKEN_KIND, kind)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, jti, exp);
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            throw new UnauthorizedException(ErrorMessages.JWT_INVALID);
        }
    }

    @SuppressWarnings("unchecked")
    private HabitatPrincipal toPrincipal(Claims claims) {
        UUID id = UUID.fromString(claims.get(JwtConstants.CLAIM_USER_ID, String.class));
        String email = claims.get(JwtConstants.CLAIM_EMAIL, String.class);
        Set<Role> roles = ((java.util.List<String>) claims.get(JwtConstants.CLAIM_ROLES))
                .stream().map(Role::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
        Role active = Role.valueOf(claims.get(JwtConstants.CLAIM_ACTIVE_ROLE, String.class));
        Instant expiresAt = claims.getExpiration() == null ? null : claims.getExpiration().toInstant();
        return new HabitatPrincipal(id, email, roles, active, claims.getId(), expiresAt);
    }

    public record IssuedToken(String token, String jti, Instant expiresAt) {}
}
