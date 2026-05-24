package com.example.common.exception;

/**
 * Thrown when authorization service is unavailable (circuit breaker open)
 */
public class AuthorizationServiceUnavailableException extends PaymentException {
    public AuthorizationServiceUnavailableException(String message) {
        super("AUTH_SERVICE_UNAVAILABLE", message, 503);
    }

    public AuthorizationServiceUnavailableException(String message, Throwable cause) {
        super("AUTH_SERVICE_UNAVAILABLE", message, 503, cause);
    }
}
