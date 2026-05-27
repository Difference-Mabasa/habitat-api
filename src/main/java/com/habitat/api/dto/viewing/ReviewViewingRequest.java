package com.habitat.api.dto.viewing;

import jakarta.validation.constraints.Size;

/**
 * Manager-side body for approve/reject. The decision note is
 * optional — empty bodies are valid (no message attached).
 */
public record ReviewViewingRequest(
        @Size(max = 1000) String decisionNote
) {}
