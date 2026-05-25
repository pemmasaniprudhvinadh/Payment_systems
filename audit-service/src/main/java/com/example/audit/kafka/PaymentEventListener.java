package com.example.audit.kafka;

import com.example.audit.model.AuditLog;
import com.example.audit.repository.AuditLogRepository;
import com.example.common.events.PaymentEvents;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private final AuditLogRepository auditLogRepository;

    public PaymentEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Listen for payment requested events and log to audit trail
     */
    @KafkaListener(topics = "payment-events", groupId = "audit-service")
    @CircuitBreaker(name = "auditCircuitBreaker", fallbackMethod = "auditFallback")
    @Retry(name = "auditRetry")
    public void onPaymentRequested(PaymentEvents.PaymentRequested event) {
        try {
            System.out.println("Audit Service: Logging payment requested event - " + event.paymentId);
            AuditLog log = new AuditLog(
                event.paymentId,
                event.userId,
                "PAYMENT_REQUESTED",
                "Payment requested by user " + event.userId + " for amount " + event.amount,
                event.amount,
                event.currency,
                "INITIATED"
            );
            auditLogRepository.save(log);
            System.out.println("✓ Audit log saved: " + event.paymentId);
        } catch (Exception e) {
            System.err.println("Error logging payment requested event: " + e.getMessage());
            throw new RuntimeException("Audit logging failed", e);
        }
    }

    /**
     * Listen for fraud check completed events and log to audit trail
     */
    @KafkaListener(topics = "payment-events", groupId = "audit-service")
    @CircuitBreaker(name = "auditCircuitBreaker", fallbackMethod = "auditFallback")
    @Retry(name = "auditRetry")
    public void onFraudCheckCompleted(PaymentEvents.FraudCheckCompleted event) {
        try {
            System.out.println("Audit Service: Logging fraud check result - " + event.paymentId);
            AuditLog log = new AuditLog(
                event.paymentId,
                event.userId,
                "FRAUD_CHECK_COMPLETED",
                "Fraud check " + (event.isFraudulent ? "FAILED" : "PASSED") + ". Reason: " + event.reason + ". Risk Score: " + event.riskScore,
                null,
                null,
                event.isFraudulent ? "FRAUD_DETECTED" : "PASSED"
            );
            auditLogRepository.save(log);
            System.out.println("✓ Fraud audit log saved: " + event.paymentId);
        } catch (Exception e) {
            System.err.println("Error logging fraud check event: " + e.getMessage());
            throw new RuntimeException("Audit logging failed", e);
        }
    }

    /**
     * Listen for payment authorized events and log to audit trail
     */
    @KafkaListener(topics = "payment-events", groupId = "audit-service")
    @CircuitBreaker(name = "auditCircuitBreaker", fallbackMethod = "auditFallback")
    @Retry(name = "auditRetry")
    public void onPaymentAuthorized(PaymentEvents.PaymentAuthorized event) {
        try {
            System.out.println("Audit Service: Logging payment authorization - " + event.paymentId);
            AuditLog log = new AuditLog(
                event.paymentId,
                event.userId,
                "PAYMENT_AUTHORIZED",
                "Payment authorization " + (event.authorized ? "APPROVED" : "DECLINED") + ". Auth Code: " + event.authCode,
                null,
                null,
                event.authorized ? "AUTHORIZED" : "DECLINED"
            );
            auditLogRepository.save(log);
            System.out.println("✓ Authorization audit log saved: " + event.paymentId);
        } catch (Exception e) {
            System.err.println("Error logging payment authorization event: " + e.getMessage());
            throw new RuntimeException("Audit logging failed", e);
        }
    }

    /**
     * Listen for payment settled events and log to audit trail
     */
    @KafkaListener(topics = "payment-events", groupId = "audit-service")
    @CircuitBreaker(name = "auditCircuitBreaker", fallbackMethod = "auditFallback")
    @Retry(name = "auditRetry")
    public void onPaymentSettled(PaymentEvents.PaymentSettled event) {
        try {
            System.out.println("Audit Service: Logging payment settlement - " + event.paymentId);
            AuditLog log = new AuditLog(
                event.paymentId,
                event.userId,
                "PAYMENT_SETTLED",
                "Payment settled successfully. Transaction ID: " + event.transactionId,
                null,
                null,
                "COMPLETED"
            );
            auditLogRepository.save(log);
            System.out.println("✓ Settlement audit log saved: " + event.paymentId);
        } catch (Exception e) {
            System.err.println("Error logging payment settlement event: " + e.getMessage());
            throw new RuntimeException("Audit logging failed", e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    public void auditFallback(PaymentEvents.PaymentRequested event, Exception e) {
        System.err.println("Audit service circuit breaker OPEN: Cannot log payment request");
        System.err.println("Reason: " + e.getMessage());
        // In production: queue audit logs for retry or send to DLQ
    }

    public void auditFallback(PaymentEvents.FraudCheckCompleted event, Exception e) {
        System.err.println("Audit service circuit breaker OPEN: Cannot log fraud check event");
    }

    public void auditFallback(PaymentEvents.PaymentAuthorized event, Exception e) {
        System.err.println("Audit service circuit breaker OPEN: Cannot log authorization event");
    }

    public void auditFallback(PaymentEvents.PaymentSettled event, Exception e) {
        System.err.println("Audit service circuit breaker OPEN: Cannot log settlement event");
    }
}
