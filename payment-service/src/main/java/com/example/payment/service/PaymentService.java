package com.example.payment.service;

import com.example.common.events.PaymentEvents;
import com.example.common.exception.*;
import com.example.payment.model.Payment;
import com.example.payment.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Initiates a payment request with idempotency and card tokenization
     * Includes validation and error handling
     */
    public Payment initiatePayment(String userId, String tokenizedCardId, BigDecimal amount, 
                                   String currency, String merchantId, String idempotencyKey) {
        try {
            // Check for duplicate request (idempotency)
            if (idempotencyKey != null) {
                var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
                if (existingPayment.isPresent()) {
                    System.out.println("Duplicate payment request detected (idempotency): " + idempotencyKey);
                    return existingPayment.get();
                }
            }

            // Validate request
            validatePaymentRequest(userId, tokenizedCardId, amount);

            // Generate idempotency key if not provided
            String idempotencyKeyValue = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();

            // Create payment record
            String paymentId = UUID.randomUUID().toString();
            Payment payment = new Payment(paymentId, idempotencyKeyValue, userId, tokenizedCardId, 
                                         amount, currency, merchantId);
            payment.setStatus(Payment.PaymentStatus.FRAUD_CHECK_PENDING);
            Payment savedPayment = paymentRepository.save(payment);

            // Publish PaymentRequested event to Kafka with error handling
            publishPaymentRequestedEvent(paymentId, idempotencyKeyValue, userId, tokenizedCardId, 
                                        amount, currency, merchantId);

            return savedPayment;
        } catch (PaymentValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("PAYMENT_INITIATION_FAILED", 
                "Failed to initiate payment: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Validate payment request parameters
     */
    private void validatePaymentRequest(String userId, String tokenizedCardId, BigDecimal amount) {
        if (userId == null || userId.isEmpty()) {
            throw new PaymentValidationException("User ID is required");
        }
        if (tokenizedCardId == null || tokenizedCardId.isEmpty()) {
            throw new PaymentValidationException("Tokenized card ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Amount must be greater than 0");
        }
        if (amount.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            throw new PaymentValidationException("Amount exceeds maximum limit of 1,000,000");
        }
    }

    /**
     * Publish PaymentRequested event with error handling
     */
    private void publishPaymentRequestedEvent(String paymentId, String idempotencyKey, String userId, 
                                             String tokenizedCardId, BigDecimal amount, 
                                             String currency, String merchantId) {
        try {
            PaymentEvents.PaymentRequested event = new PaymentEvents.PaymentRequested(
                paymentId, idempotencyKey, userId, tokenizedCardId, amount, currency, merchantId
            );
            kafkaTemplate.send("payment-requested", paymentId, event).get();
            System.out.println("PaymentRequested event published for paymentId: " + paymentId);
        } catch (Exception e) {
            System.err.println("Failed to publish PaymentRequested event: " + e.getMessage());
            throw new PaymentException("EVENT_PUBLISHING_FAILED", 
                "Failed to publish payment event: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Update payment with fraud check result
     * Protected with error handling
     */
    public void updateFraudCheck(String paymentId, boolean isFraudulent, String reason, double riskScore) {
        try {
            var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            if (isFraudulent) {
                payment.setStatus(Payment.PaymentStatus.FRAUD_DETECTED);
                System.out.println("Payment flagged as fraudulent: " + paymentId + " Reason: " + reason);
            } else {
                payment.setStatus(Payment.PaymentStatus.AUTHORIZED);
                System.out.println("Payment passed fraud check: " + paymentId);
            }
            payment.setFraudCheckStatus(isFraudulent ? "FLAGGED" : "PASSED");
            payment.setFraudRiskScore(riskScore);
            paymentRepository.save(payment);
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error updating fraud check for payment " + paymentId + ": " + e.getMessage());
            throw new PaymentException("FRAUD_CHECK_UPDATE_FAILED", 
                "Failed to update fraud check status: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Update payment with authorization result
     * Protected with error handling
     */
    public void updateAuthorization(String paymentId, boolean authorized, String authCode, String reason) {
        try {
            var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            if (authorized) {
                payment.setStatus(Payment.PaymentStatus.SETTLEMENT_PENDING);
                payment.setAuthCode(authCode);
                System.out.println("Payment authorized: " + paymentId + " AuthCode: " + authCode);
            } else {
                payment.setStatus(Payment.PaymentStatus.AUTHORIZATION_FAILED);
                System.out.println("Payment authorization failed: " + paymentId + " Reason: " + reason);
            }
            paymentRepository.save(payment);
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error updating authorization for payment " + paymentId + ": " + e.getMessage());
            throw new PaymentException("AUTHORIZATION_UPDATE_FAILED", 
                "Failed to update authorization status: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Update payment with settlement result
     * Protected with error handling
     */
    public void updateSettlement(String paymentId, String settlementId, String status, String transactionId) {
        try {
            var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            if ("COMPLETED".equals(status)) {
                payment.setStatus(Payment.PaymentStatus.SETTLED);
                System.out.println("Payment settled successfully: " + paymentId + " TxnId: " + transactionId);
            } else if ("FAILED".equals(status)) {
                payment.setStatus(Payment.PaymentStatus.SETTLEMENT_FAILED);
                System.out.println("Payment settlement failed: " + paymentId);
            }
            payment.setSettlementId(settlementId);
            payment.setTransactionId(transactionId);
            paymentRepository.save(payment);
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error updating settlement for payment " + paymentId + ": " + e.getMessage());
            throw new PaymentException("SETTLEMENT_UPDATE_FAILED", 
                "Failed to update settlement status: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Get payment with error handling
     */
    public Payment getPayment(String paymentId) {
        try {
            return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("PAYMENT_RETRIEVAL_FAILED", 
                "Failed to retrieve payment: " + e.getMessage(), 500, e);
        }
    }
}
