package com.example.common.exception;

/**
 * Thrown when payment is not found
 */
public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(String paymentId) {
        super("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId, 404);
    }

    public PaymentNotFoundException(String message, Throwable cause) {
        super("PAYMENT_NOT_FOUND", message, 404, cause);
    }
}
