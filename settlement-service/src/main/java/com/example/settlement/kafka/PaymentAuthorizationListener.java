package com.example.settlement.kafka;

import com.example.common.events.PaymentEvents;
import com.example.settlement.service.SettlementService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentAuthorizationListener {

    private final SettlementService settlementService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentAuthorizationListener(SettlementService settlementService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.settlementService = settlementService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "payment-events", groupId = "settlement-service")
    @CircuitBreaker(name = "settlementProcessingCircuitBreaker", fallbackMethod = "settlementProcessingFallback")
    @Retry(name = "settlementProcessingRetry")
    public void onPaymentAuthorized(PaymentEvents.PaymentAuthorized event) {
        try {
            System.out.println("Settlement Service: Processing authorized payment " + event.paymentId);

            if (!event.authorized) {
                System.out.println("Payment authorization failed, skipping settlement for: " + event.paymentId);
                return;
            }

            // Initiate settlement
            var settlement = settlementService.initiateSettlement(event.paymentId, 
                /* amount would come from payment service in real scenario */ null);

            System.out.println("Settlement initiated for payment: " + event.paymentId);

            // Simulate settlement processing with circuit breaker protection
            simulateSettlementProcessing(event.paymentId);

        } catch (Exception e) {
            System.err.println("Settlement processing error for payment " + event.paymentId + ": " + e.getMessage());
            throw new RuntimeException("Settlement processing failed", e);
        }
    }

    /**
     * Simulate settlement processing (actual implementation would call payment processor)
     */
    private void simulateSettlementProcessing(String paymentId) {
        try {
            // Simulate processing delay
            Thread.sleep(1000);

            // Randomly succeed or fail (in production, this would be actual settlement)
            boolean success = Math.random() > 0.1; // 90% success rate

            if (success) {
                var settlement = settlementService.completeSettlement(paymentId);
                System.out.println("Settlement completed for payment: " + paymentId + 
                                 " transactionId: " + settlement.getTransactionId());

                // Publish settlement completed event
                publishSettlementResult(paymentId, settlement.getId(), "COMPLETED", settlement.getTransactionId());
            } else {
                var settlement = settlementService.failSettlement(paymentId, "Settlement processing error");
                System.out.println("Settlement failed for payment: " + paymentId);

                // Publish settlement failed event
                publishSettlementResult(paymentId, settlement.getId(), "FAILED", null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Settlement processing interrupted for payment: " + paymentId);
        } catch (Exception e) {
            System.err.println("Error during settlement simulation: " + e.getMessage());
            throw new RuntimeException("Settlement simulation failed", e);
        }
    }

    /**
     * Publish settlement result with error handling
     */
    private void publishSettlementResult(String paymentId, String settlementId, String status, String transactionId) {
        try {
            PaymentEvents.PaymentSettled settledEvent = new PaymentEvents.PaymentSettled(
                paymentId,
                settlementId,
                status,
                transactionId
            );
            kafkaTemplate.send("payment-events", paymentId, settledEvent).get();
            System.out.println("PaymentSettled event published for paymentId: " + paymentId);
        } catch (Exception e) {
            System.err.println("Failed to publish settlement result: " + e.getMessage());
            throw new RuntimeException("Failed to publish settlement event", e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    public void settlementProcessingFallback(PaymentEvents.PaymentAuthorized event, Exception e) {
        System.err.println("Circuit breaker OPEN: Cannot process settlement for payment " + event.paymentId);
        System.err.println("Reason: " + e.getMessage());
        
        // In production, could send to a dead letter queue for manual processing
        // For now, we'll just log the failure
        System.err.println("Settlement will be retried when circuit breaker recovers");
    }
}
