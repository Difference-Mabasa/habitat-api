package com.habitat.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.habitat.api.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Self-registration payload. Role is explicit but AuthService rejects ADMIN /
 * SUPER_ADMIN — the only privileged path is by ADMIN-initiated user creation.
 *
 * The @JsonCreator + role whitelist mean even a crafted payload can't slip
 * through; we re-check in the service for belt-and-braces (lesson SEC-13
 * from backroom).
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(min = 1, max = 80) String displayName,
        @NotNull Role role,
        String area
) {
    @JsonCreator
    public RegisterRequest(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("role") Role role,
            @JsonProperty("area") String area
    ) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        // Reject privileged role at deserialization time as the first line of defence.
        // The service re-asserts.
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) {
            this.role = null;
        } else {
            this.role = role;
        }
        this.area = area;
    }
}
