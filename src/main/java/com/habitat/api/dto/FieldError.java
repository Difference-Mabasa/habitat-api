package com.habitat.api.dto;

/** One row of {@link ApiError#errors()} — only populated for HTTP 422 responses. */
public record FieldError(String field, String message) {}
