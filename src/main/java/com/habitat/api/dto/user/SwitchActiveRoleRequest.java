package com.habitat.api.dto.user;

import com.habitat.api.enums.Role;
import jakarta.validation.constraints.NotNull;

public record SwitchActiveRoleRequest(@NotNull Role role) {}
