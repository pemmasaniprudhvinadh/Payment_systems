package com.example.common.exception;

/**
 * Thrown when settlement service is unavailable (circuit breaker open)
 */
public class SettlementServiceUnavailableException extends PaymentException {
    public SettlementServiceUnavailableException(String message) {
        super("SETTLEMENT_SERVICE_UNAVAILABLE", message, 503);
    }

    public SettlementServiceUnavailableException(String message, Throwable cause) {
        super("SETTLEMENT_SERVICE_UNAVAILABLE", message, 503, cause);
    }
}
