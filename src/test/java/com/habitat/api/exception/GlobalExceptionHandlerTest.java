package com.habitat.api.exception;

import com.habitat.api.dto.ApiError;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-unit coverage of the handler-method-to-ApiError contract.
 *
 * The 409 STALE_RESOURCE path is the BUG-01 surface; the other paths
 * are pinned here so reformatting GlobalExceptionHandler can't change
 * a status code or error code silently.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimistic_lock_jpa_variant_maps_to_409_stale_resource() {
        ResponseEntity<ApiError> response = handler.handleOptimisticLock(
                new OptimisticLockException("collision"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.code()).isEqualTo("STALE_RESOURCE");
        assertThat(body.message())
                .isEqualTo("This resource was modified by someone else. Refresh and try again.");
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void optimistic_lock_spring_variant_maps_to_409_stale_resource() {
        ResponseEntity<ApiError> response = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("collision"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("STALE_RESOURCE");
    }

    @Test
    void api_exception_uses_its_own_status_and_code() {
        ResponseEntity<ApiError> response = handler.handleApi(
                new ResourceNotFoundException("Foo missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Foo missing");
    }

    @Test
    void api_exception_5xx_still_returns_its_own_status() {
        ResponseEntity<ApiError> response = handler.handleApi(
                new ServiceUnavailableException("storage down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SERVICE_UNAVAILABLE");
    }

    @Test
    void data_integrity_violation_maps_to_409_conflict() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("dup key"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
    }

    @Test
    void access_denied_maps_to_403() {
        ResponseEntity<ApiError> response = handler.handleAccessDenied(
                new AccessDeniedException("no"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void no_handler_maps_to_404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new NoHandlerFoundException("GET", "/missing", HttpHeaders.EMPTY));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void unexpected_exception_maps_to_500_internal_error() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
