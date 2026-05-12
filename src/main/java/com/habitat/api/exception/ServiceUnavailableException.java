package com.habitat.api.exception;

import org.springframework.http.HttpStatus;

/**
 * For dependency outages — storage unavailable, payment provider down, etc.
 * Maps to 503 so clients can implement sensible retry strategies.
 */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", message, cause);
    }
}
