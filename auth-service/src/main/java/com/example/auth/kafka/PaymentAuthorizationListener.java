package com.example.auth.kafka;

import com.example.common.events.PaymentEvents;
import com.example.common.exception.PaymentException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentAuthorizationListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentAuthorizationListener(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Listen for fraud check completed events and authorize the payment if not fraudulent
     */
    @KafkaListener(topics = "payment-events", groupId = "auth-service")
    @CircuitBreaker(name = "authorizationCircuitBreaker", fallbackMethod = "authorizationFallback")
    @Retry(name = "authorizationRetry")
    public void onFraudCheckCompleted(PaymentEvents.FraudCheckCompleted event) {
        try {
            System.out.println("Auth Service: Processing fraud check result for payment " + event.paymentId + 
                              " isFraudulent=" + event.isFraudulent);

            if (event.isFraudulent) {
                // Publish authorization failure
                publishAuthorizationResult(event.paymentId, false, null, 
                    "Fraud detected: " + event.reason);
                return;
            }

            // Authorize the payment
            boolean authorized = authorizePayment(event.paymentId);
            String authCode = authorized ? "AUTH_" + UUID.randomUUID().toString().substring(0, 8) : null;
            String reason = authorized ? "Payment authorized" : "Authorization declined";

            System.out.println("Payment authorization result: " + (authorized ? "AUTHORIZED" : "DECLINED"));

            // Publish authorization result
            publishAuthorizationResult(event.paymentId, authorized, authCode, reason);

        } catch (Exception e) {
            System.err.println("Error processing fraud check for payment " + event.paymentId + ": " + e.getMessage());
            throw new RuntimeException("Authorization processing failed", e);
        }
    }

    /**
     * Authorize a payment (simple logic - in production, integrate with payment processor)
     */
    private boolean authorizePayment(String paymentId) {
        try {
            // Simulate authorization (90% success rate)
            return Math.random() > 0.1;
        } catch (Exception e) {
            throw new PaymentException("AUTHORIZATION_PROCESSING_ERROR", 
                "Error during authorization: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Publish authorization result with error handling
     */
    private void publishAuthorizationResult(String paymentId, boolean authorized, String authCode, String reason) {
        try {
            PaymentEvents.PaymentAuthorized authEvent = new PaymentEvents.PaymentAuthorized(
                paymentId,
                authorized,
                authCode,
                reason
            );
            kafkaTemplate.send("payment-events", paymentId, authEvent).get();
            System.out.println("PaymentAuthorized event published for paymentId: " + paymentId);
        } catch (Exception e) {
            System.err.println("Failed to publish authorization result: " + e.getMessage());
            throw new RuntimeException("Failed to publish authorization event", e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    public void authorizationFallback(PaymentEvents.FraudCheckCompleted event, Exception e) {
        System.err.println("Circuit breaker OPEN: Cannot process authorization for payment " + event.paymentId);
        System.err.println("Reason: " + e.getMessage());
        
        // Publish a default authorization result to allow payment to continue (fail open)
        try {
            PaymentEvents.PaymentAuthorized authEvent = new PaymentEvents.PaymentAuthorized(
                event.paymentId,
                true,  // Authorize by default when service is down (fail open)
                "AUTH_DEFAULT",
                "Authorization service temporarily unavailable - defaulting to authorize"
            );
            kafkaTemplate.send("payment-events", event.paymentId, authEvent).get();
            System.out.println("Published fallback authorization result for paymentId: " + event.paymentId);
        } catch (Exception ex) {
            System.err.println("Failed to publish fallback authorization result: " + ex.getMessage());
        }
    }
}
