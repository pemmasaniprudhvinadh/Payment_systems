package com.example.common.exception;

/**
 * Thrown when payment validation fails
 */
public class PaymentValidationException extends PaymentException {
    public PaymentValidationException(String message) {
        super("INVALID_PAYMENT_REQUEST", message, 400);
    }

    public PaymentValidationException(String message, Throwable cause) {
        super("INVALID_PAYMENT_REQUEST", message, 400, cause);
    }
}
