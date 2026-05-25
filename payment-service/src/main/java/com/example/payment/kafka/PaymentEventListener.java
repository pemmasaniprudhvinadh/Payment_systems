package com.example.payment.kafka;

import com.example.common.events.PaymentEvents;
import com.example.payment.service.PaymentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private final PaymentService paymentService;

    public PaymentEventListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "payment-events", groupId = "payment-service")
    @CircuitBreaker(name = "fraudCheckCircuitBreaker", fallbackMethod = "fraudCheckCompletedFallback")
    @Retry(name = "fraudCheckRetry")
    public void onFraudCheckCompleted(PaymentEvents.FraudCheckCompleted event) {
        try {
            System.out.println("Received FraudCheckCompleted for paymentId=" + event.paymentId + 
                              " isFraudulent=" + event.isFraudulent + " riskScore=" + event.riskScore);
            paymentService.updateFraudCheck(event.paymentId, event.isFraudulent, event.reason, event.riskScore);
        } catch (Exception e) {
            System.err.println("Error processing fraud check event for payment " + event.paymentId + ": " + e.getMessage());
            throw new RuntimeException("Failed to process fraud check event", e);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "payment-service")
    @CircuitBreaker(name = "authorizationCircuitBreaker", fallbackMethod = "paymentAuthorizedFallback")
    @Retry(name = "authorizationRetry")
    public void onPaymentAuthorized(PaymentEvents.PaymentAuthorized event) {
        try {
            System.out.println("Received PaymentAuthorized for paymentId=" + event.paymentId + 
                              " authorized=" + event.authorized);
            paymentService.updateAuthorization(event.paymentId, event.authorized, event.authCode, event.reason);
        } catch (Exception e) {
            System.err.println("Error processing authorization event for payment " + event.paymentId + ": " + e.getMessage());
            throw new RuntimeException("Failed to process authorization event", e);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "payment-service")
    @CircuitBreaker(name = "settlementCircuitBreaker", fallbackMethod = "paymentSettledFallback")
    @Retry(name = "settlementRetry")
    public void onPaymentSettled(PaymentEvents.PaymentSettled event) {
        try {
            System.out.println("Received PaymentSettled for paymentId=" + event.paymentId + 
                              " status=" + event.status + " transactionId=" + event.transactionId);
            paymentService.updateSettlement(event.paymentId, event.settlementId, event.status, event.transactionId);
        } catch (Exception e) {
            System.err.println("Error processing settlement event for payment " + event.paymentId + ": " + e.getMessage());
            throw new RuntimeException("Failed to process settlement event", e);
        }
    }

    /**
     * Fallback method for fraud check events
     */
    public void fraudCheckCompletedFallback(PaymentEvents.FraudCheckCompleted event, Exception e) {
        System.err.println("Circuit breaker OPEN: Cannot process fraud check for payment " + event.paymentId);
        System.err.println("Reason: " + e.getMessage());
        // In production, could send to a dead letter queue or alert operations
    }

    /**
     * Fallback method for authorization events
     */
    public void paymentAuthorizedFallback(PaymentEvents.PaymentAuthorized event, Exception e) {
        System.err.println("Circuit breaker OPEN: Cannot process authorization for payment " + event.paymentId);
        System.err.println("Reason: " + e.getMessage());
        // In production, could send to a dead letter queue or alert operations
    }

    /**
     * Fallback method for settlement events
     */
    public void paymentSettledFallback(PaymentEvents.PaymentSettled event, Exception e) {
        System.err.println("Circuit breaker OPEN: Cannot process settlement for payment " + event.paymentId);
        System.err.println("Reason: " + e.getMessage());
        // In production, could send to a dead letter queue or alert operations
    }
}
