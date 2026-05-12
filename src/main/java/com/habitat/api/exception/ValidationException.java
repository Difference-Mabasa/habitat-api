package com.habitat.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Distinct from {@link BadRequestException} — used when a request is well-formed
 * but fails business validation (e.g. semantic checks that go beyond what
 * Jakarta validation can express). Maps to 422.
 */
public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", message);
    }
}
