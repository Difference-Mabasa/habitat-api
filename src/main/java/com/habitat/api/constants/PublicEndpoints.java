package com.habitat.api.constants;

import java.util.List;

/**
 * The single source of truth for unauthenticated endpoints.
 * SecurityConfig references this list — never inline paths there.
 */
public final class PublicEndpoints {

    public static final List<String> PATHS = List.of(
            ApiRoutes.AUTH_REGISTER,
            ApiRoutes.AUTH_LOGIN,
            ApiRoutes.AUTH_REFRESH,
            ApiRoutes.AUTH_OAUTH2_EXCHANGE,
            ApiRoutes.HEALTH,
            ApiRoutes.DOCS,
            ApiRoutes.DOCS_API,
            "/actuator/health",
            "/error"
    );

    private PublicEndpoints() {}
}
