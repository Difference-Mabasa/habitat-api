package com.habitat.api.constants;

public final class JwtConstants {

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ROLE_PREFIX = "ROLE_";

    // Claims
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_ACTIVE_ROLE = "activeRole";
    public static final String CLAIM_JTI = "jti";

    // Token kinds
    public static final String TOKEN_KIND_ACCESS = "access";
    public static final String TOKEN_KIND_REFRESH = "refresh";
    public static final String CLAIM_TOKEN_KIND = "kind";

    // Redis blocklist key prefix — jti is appended.
    public static final String BLOCKLIST_KEY_PREFIX = "jwt:blocklist:";

    private JwtConstants() {}
}
