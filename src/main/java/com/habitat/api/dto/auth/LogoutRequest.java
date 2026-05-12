package com.habitat.api.dto.auth;

/**
 * Logout body is optional — when the client sends the refresh token, we
 * revoke it too so the session is fully terminated. With no body we still
 * revoke the access token via the Authorization header.
 */
public record LogoutRequest(String refreshToken) {}
