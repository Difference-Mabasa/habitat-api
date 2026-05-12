package com.habitat.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base for every exception thrown from a service or controller.
 *
 * Concrete subclasses set the HTTP status; GlobalExceptionHandler maps each
 * subclass to a typed ApiError response.
 *
 * Rule (enforced by the pre-commit hook + ArchUnit): production code never
 * throws bare {@code RuntimeException} — always one of these.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    protected ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
