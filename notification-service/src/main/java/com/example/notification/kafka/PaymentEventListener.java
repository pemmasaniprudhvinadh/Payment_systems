package com.example.notification.kafka;

import com.example.common.events.PaymentEvents;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    /**
     * Listen for payment requested events and send notifications
     */
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    @CircuitBreaker(name = "notificationCircuitBreaker", fallbackMethod = "notificationFallback")
    @Retry(name = "notificationRetry")
    public void onPaymentRequested(PaymentEvents.PaymentRequested event) {
        try {
            System.out.println("Notification Service: Payment requested for user " + event.userId);
            sendEmailNotification(event.userId, 
                "Payment Initiated", 
                "Your payment of " + event.amount + " " + event.currency + " has been initiated. Payment ID: " + event.paymentId);
        } catch (Exception e) {
            System.err.println("Error sending notification for payment request: " + e.getMessage());
            throw new RuntimeException("Notification sending failed", e);
        }
    }

    /**
     * Listen for fraud check completed events and send notifications
     */
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    @CircuitBreaker(name = "notificationCircuitBreaker", fallbackMethod = "notificationFallback")
    @Retry(name = "notificationRetry")
    public void onFraudCheckCompleted(PaymentEvents.FraudCheckCompleted event) {
        try {
            System.out.println("Notification Service: Fraud check completed for payment " + event.paymentId);
            if (event.isFraudulent) {
                sendEmailNotification(event.userId, 
                    "Fraud Alert", 
                    "Payment " + event.paymentId + " has been flagged as fraudulent. Reason: " + event.reason);
            }
        } catch (Exception e) {
            System.err.println("Error sending fraud notification: " + e.getMessage());
            throw new RuntimeException("Fraud notification failed", e);
        }
    }

    /**
     * Listen for payment authorized events and send notifications
     */
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    @CircuitBreaker(name = "notificationCircuitBreaker", fallbackMethod = "notificationFallback")
    @Retry(name = "notificationRetry")
    public void onPaymentAuthorized(PaymentEvents.PaymentAuthorized event) {
        try {
            System.out.println("Notification Service: Payment authorized - " + event.paymentId);
            if (event.authorized) {
                sendEmailNotification(event.userId, 
                    "Payment Authorized", 
                    "Your payment " + event.paymentId + " has been authorized. Authorization Code: " + event.authCode);
            }
        } catch (Exception e) {
            System.err.println("Error sending authorization notification: " + e.getMessage());
            throw new RuntimeException("Authorization notification failed", e);
        }
    }

    /**
     * Listen for payment settled events and send notifications
     */
    @KafkaListener(topics = "payment-events", groupId = "notification-service")
    @CircuitBreaker(name = "notificationCircuitBreaker", fallbackMethod = "notificationFallback")
    @Retry(name = "notificationRetry")
    public void onPaymentSettled(PaymentEvents.PaymentSettled event) {
        try {
            System.out.println("Notification Service: Payment settled - " + event.paymentId);
            sendEmailNotification(event.userId, 
                "Payment Completed", 
                "Your payment " + event.paymentId + " has been completed successfully. Transaction ID: " + event.transactionId);
        } catch (Exception e) {
            System.err.println("Error sending settlement notification: " + e.getMessage());
            throw new RuntimeException("Settlement notification failed", e);
        }
    }

    /**
     * Send email notification (stub implementation)
     */
    private void sendEmailNotification(String userId, String subject, String message) {
        // In production, integrate with email service (SendGrid, AWS SES, etc.)
        System.out.println("📧 Email sent to user " + userId);
        System.out.println("   Subject: " + subject);
        System.out.println("   Message: " + message);
    }

    /**
     * Fallback method when circuit breaker is open
     */
    public void notificationFallback(PaymentEvents.PaymentRequested event, Exception e) {
        System.err.println("Notification service circuit breaker OPEN: Cannot send notifications");
        System.err.println("Reason: " + e.getMessage());
        // In production: queue notifications for retry or send to DLQ
    }

    public void notificationFallback(PaymentEvents.FraudCheckCompleted event, Exception e) {
        System.err.println("Notification service circuit breaker OPEN: Cannot send fraud notification");
    }

    public void notificationFallback(PaymentEvents.PaymentAuthorized event, Exception e) {
        System.err.println("Notification service circuit breaker OPEN: Cannot send authorization notification");
    }

    public void notificationFallback(PaymentEvents.PaymentSettled event, Exception e) {
        System.err.println("Notification service circuit breaker OPEN: Cannot send settlement notification");
    }
}
