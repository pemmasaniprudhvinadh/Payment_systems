package com.example.fraud.kafka;

import com.example.common.events.PaymentEvents;
import com.example.fraud.service.FraudDetectionService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestListener {

    private final FraudDetectionService fraudDetectionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentRequestListener(FraudDetectionService fraudDetectionService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.fraudDetectionService = fraudDetectionService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "payment-requested", groupId = "fraud-service")
    @CircuitBreaker(name = "fraudAnalysisCircuitBreaker", fallbackMethod = "fraudAnalysisFallback")
    @Retry(name = "fraudAnalysisRetry")
    public void onPaymentRequested(PaymentEvents.PaymentRequested event) {
        try {
            System.out.println("Fraud Service: Processing payment " + event.paymentId + 
                              " for user " + event.userId + " amount: " + event.amount);

            // Perform fraud analysis
            FraudDetectionService.FraudAnalysis analysis = fraudDetectionService.analyzeFraud(
                event.paymentId,
                event.userId,
                event.tokenizedCardId,
                event.amount,
                event.currency
            );

            System.out.println("Fraud analysis result: isFraudulent=" + analysis.isFraudulent + 
                              " riskScore=" + analysis.riskScore);

            // Publish fraud check result
            publishFraudCheckResult(event.paymentId, analysis);

        } catch (Exception e) {
            System.err.println("Error analyzing fraud for payment " + event.paymentId + ": " + e.getMessage());
            throw new RuntimeException("Fraud analysis failed", e);
        }
    }

    /**
     * Publish fraud check result with error handling
     */
    private void publishFraudCheckResult(String paymentId, FraudDetectionService.FraudAnalysis analysis) {
        try {
            PaymentEvents.FraudCheckCompleted fraudEvent = new PaymentEvents.FraudCheckCompleted(
                paymentId,
                analysis.isFraudulent,
                analysis.reason,
                analysis.riskScore
            );
            kafkaTemplate.send("fraud-check-completed", paymentId, fraudEvent).get();
            System.out.println("FraudCheckCompleted event published for paymentId: " + paymentId);
        } catch (Exception e) {
            System.err.println("Failed to publish fraud check result: " + e.getMessage());
            throw new RuntimeException("Failed to publish fraud check event", e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    public void fraudAnalysisFallback(PaymentEvents.PaymentRequested event, Exception e) {
        System.err.println("Circuit breaker OPEN: Cannot process payment for fraud analysis: " + event.paymentId);
        System.err.println("Reason: " + e.getMessage());
        
        // Publish a default fraud check result indicating service unavailable
        try {
            PaymentEvents.FraudCheckCompleted fraudEvent = new PaymentEvents.FraudCheckCompleted(
                event.paymentId,
                false,  // Assume not fraudulent to allow payment to proceed
                "Fraud service temporarily unavailable - defaulting to pass",
                0.0
            );
            kafkaTemplate.send("fraud-check-completed", event.paymentId, fraudEvent).get();
            System.out.println("Published fallback fraud check result for paymentId: " + event.paymentId);
        } catch (Exception ex) {
            System.err.println("Failed to publish fallback fraud check result: " + ex.getMessage());
        }
    }
}
