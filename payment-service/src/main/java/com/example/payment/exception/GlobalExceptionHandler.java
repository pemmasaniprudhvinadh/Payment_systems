package com.example.payment.exception;

import com.example.common.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle custom payment exceptions
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(PaymentException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getHttpStatusCode()
        );
        return ResponseEntity.status(ex.getHttpStatusCode()).body(response);
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(PaymentValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleValidationException(PaymentValidationException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            400
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle card tokenization exceptions
     */
    @ExceptionHandler(CardTokenizationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleCardTokenizationException(CardTokenizationException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            400
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle fraud check service unavailable (circuit breaker open)
     */
    @ExceptionHandler(FraudCheckServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<Map<String, Object>> handleFraudCheckUnavailable(FraudCheckServiceUnavailableException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            503
        );
        response.put("retryAfter", "Please retry after a few seconds");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle authorization service unavailable (circuit breaker open)
     */
    @ExceptionHandler(AuthorizationServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<Map<String, Object>> handleAuthorizationUnavailable(AuthorizationServiceUnavailableException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            503
        );
        response.put("retryAfter", "Please retry after a few seconds");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle settlement service unavailable (circuit breaker open)
     */
    @ExceptionHandler(SettlementServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<Map<String, Object>> handleSettlementUnavailable(SettlementServiceUnavailableException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            503
        );
        response.put("retryAfter", "Please retry after a few seconds");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle service timeout
     */
    @ExceptionHandler(ServiceTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ResponseEntity<Map<String, Object>> handleServiceTimeout(ServiceTimeoutException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            504
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    /**
     * Handle external service exceptions
     */
    @ExceptionHandler(ExternalServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ResponseEntity<Map<String, Object>> handleExternalServiceException(ExternalServiceException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            502
        );
        response.put("service", ex.getServiceName());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    /**
     * Handle payment not found
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFound(PaymentNotFoundException ex) {
        Map<String, Object> response = createErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            404
        );
        return ResponseEntity.notFound().build();
    }

    /**
     * Handle general runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = createErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred: " + ex.getMessage(),
            500
        );
        ex.printStackTrace(); // Log for debugging
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> response = createErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            500
        );
        ex.printStackTrace(); // Log for debugging
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, int httpStatus) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", errorCode);
        response.put("message", message);
        response.put("httpStatus", httpStatus);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}
