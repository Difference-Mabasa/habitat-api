package com.habitat.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The single error shape returned for every non-2xx response.
 *
 * Always populated by GlobalExceptionHandler — controllers and services never
 * build it directly.
 */
@Builder
public record ApiError(
        int status,
        String code,
        String message,
        String requestId,
        OffsetDateTime timestamp,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldError> errors
) {}
