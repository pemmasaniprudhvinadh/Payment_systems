package com.example.common.exception;

/**
 * Base exception for payment service errors
 */
public class PaymentException extends RuntimeException {
    private final String errorCode;
    private final int httpStatusCode;

    public PaymentException(String errorCode, String message, int httpStatusCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public PaymentException(String errorCode, String message, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
