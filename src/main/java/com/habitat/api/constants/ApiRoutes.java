package com.habitat.api.constants;

/**
 * Canonical URL paths for every endpoint. Referenced from controllers
 * (@RequestMapping) and SecurityConfig (path matchers). Never hardcode a
 * literal path string in either location.
 */
public final class ApiRoutes {

    public static final String BASE = "/api/v1";

    // Auth
    public static final String AUTH = BASE + "/auth";
    public static final String AUTH_REGISTER = AUTH + "/register";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_REFRESH = AUTH + "/refresh";
    public static final String AUTH_LOGOUT = AUTH + "/logout";
    public static final String AUTH_OAUTH2_EXCHANGE = AUTH + "/oauth2/exchange";

    // Health / docs
    public static final String HEALTH = BASE + "/health";
    public static final String DOCS = "/swagger-ui/**";
    public static final String DOCS_API = "/v3/api-docs/**";

    // Users
    public static final String USERS = BASE + "/users";
    public static final String USERS_ME = USERS + "/me";

    private ApiRoutes() {}
}
