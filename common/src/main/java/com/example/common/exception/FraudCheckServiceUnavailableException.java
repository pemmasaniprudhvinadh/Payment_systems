package com.example.common.exception;

/**
 * Thrown when fraud check service is unavailable (circuit breaker open)
 */
public class FraudCheckServiceUnavailableException extends PaymentException {
    public FraudCheckServiceUnavailableException(String message) {
        super("FRAUD_CHECK_SERVICE_UNAVAILABLE", message, 503);
    }

    public FraudCheckServiceUnavailableException(String message, Throwable cause) {
        super("FRAUD_CHECK_SERVICE_UNAVAILABLE", message, 503, cause);
    }
}
