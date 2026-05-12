package com.habitat.api.security;

import com.habitat.api.constants.JwtConstants;
import com.habitat.api.enums.Role;
import com.habitat.api.exception.UnauthorizedException;
import com.habitat.api.constants.ErrorMessages;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * The ONLY place that reads from SecurityContextHolder. Services depend on
 * this, never on the SecurityContext directly. Lessons from backroom:
 * duplicate {@code hasRole()} helpers across 8 services drifted apart over
 * time and broke an authorization check.
 */
@Component
public class SecurityUtils {

    public Optional<UUID> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return Optional.empty();
        if (auth.getPrincipal() instanceof HabitatPrincipal p) {
            return Optional.of(p.userId());
        }
        return Optional.empty();
    }

    public UUID requireUserId() {
        return currentUserId()
                .orElseThrow(() -> new UnauthorizedException(ErrorMessages.AUTH_REQUIRED));
    }

    public boolean hasRole(Role role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String want = JwtConstants.ROLE_PREFIX + role.name();
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (want.equals(ga.getAuthority())) return true;
        }
        return false;
    }

    public boolean isPrivileged() {
        return hasRole(Role.ADMIN) || hasRole(Role.SUPER_ADMIN);
    }

    public void requireRole(Role role) {
        if (!hasRole(role)) {
            throw new UnauthorizedException(ErrorMessages.FORBIDDEN);
        }
    }
}
