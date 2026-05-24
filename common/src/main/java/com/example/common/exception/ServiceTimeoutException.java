package com.example.common.exception;

/**
 * Thrown when a service timeout occurs
 */
public class ServiceTimeoutException extends PaymentException {
    public ServiceTimeoutException(String serviceName) {
        super("SERVICE_TIMEOUT", "Service timeout: " + serviceName, 504);
    }

    public ServiceTimeoutException(String serviceName, Throwable cause) {
        super("SERVICE_TIMEOUT", "Service timeout: " + serviceName, 504, cause);
    }
}
