package com.habitat.api.exception;

import com.habitat.api.dto.ApiError;
import com.habitat.api.dto.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The single point where exceptions become HTTP responses. Every error returned
 * by the API passes through here — controllers and services must never build
 * their own error responses.
 *
 * Lessons applied from backroom:
 *  - DataIntegrityViolationException → 409, not 500
 *  - Validation failures → 422 with field-level detail
 *  - Every error carries the request's correlation ID
 *  - We log at WARN for 4xx, ERROR for 5xx (and only 5xx includes stack trace)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("API exception [{}]: {}", ex.getCode(), ex.getMessage(), ex);
        } else {
            log.warn("API exception [{}]: {}", ex.getCode(), ex.getMessage());
        }
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(
                        fe.getField(),
                        fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED",
                "Request validation failed", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICT",
                "The resource already exists or violates a constraint.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied.", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                ex.getMessage() == null ? "Authentication required." : ex.getMessage(), null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoHandlerFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Route not found.", null);
    }

    /**
     * Last-resort handler — everything that isn't an ApiException ends up here.
     * If you see this in logs, the throwing code should be updated to use a
     * typed ApiException subclass instead.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", null);
    }

    private static ResponseEntity<ApiError> build(
            HttpStatus status, String code, String message, List<FieldError> errors) {
        ApiError body = ApiError.builder()
                .status(status.value())
                .code(code)
                .message(message)
                .requestId(MDC.get("requestId"))
                .timestamp(OffsetDateTime.now())
                .errors(errors == null || errors.isEmpty() ? null : errors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
