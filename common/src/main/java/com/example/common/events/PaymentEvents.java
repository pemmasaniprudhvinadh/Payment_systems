package com.example.common.events;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentEvents {
    public static class CardAdded implements Serializable {
        public String cardId;
        public String userId;
        public String maskedNumber;

        public CardAdded() {}
        public CardAdded(String cardId, String userId, String maskedNumber) {
            this.cardId = cardId;
            this.userId = userId;
            this.maskedNumber = maskedNumber;
        }
    }

    // Payment Requested - triggered by payment API
    public static class PaymentRequested implements Serializable {
        public String paymentId;
        public String idempotencyKey;
        public String userId;
        public String tokenizedCardId;
        public BigDecimal amount;
        public String currency;
        public String merchantId;
        public LocalDateTime timestamp;

        public PaymentRequested() {}
        public PaymentRequested(String paymentId, String idempotencyKey, String userId, 
                                String tokenizedCardId, BigDecimal amount, String currency, 
                                String merchantId) {
            this.paymentId = paymentId;
            this.idempotencyKey = idempotencyKey;
            this.userId = userId;
            this.tokenizedCardId = tokenizedCardId;
            this.amount = amount;
            this.currency = currency;
            this.merchantId = merchantId;
            this.timestamp = LocalDateTime.now();
        }
    }

    // Fraud Check Completed
    public static class FraudCheckCompleted implements Serializable {
        public String paymentId;
        public boolean isFraudulent;
        public String reason;
        public double riskScore;

        public FraudCheckCompleted() {}
        public FraudCheckCompleted(String paymentId, boolean isFraudulent, String reason, double riskScore) {
            this.paymentId = paymentId;
            this.isFraudulent = isFraudulent;
            this.reason = reason;
            this.riskScore = riskScore;
        }
    }

    // Authorization Completed
    public static class PaymentAuthorized implements Serializable {
        public String paymentId;
        public boolean authorized;
        public String authCode;
        public String reason;

        public PaymentAuthorized() {}
        public PaymentAuthorized(String paymentId, boolean authorized, String authCode, String reason) {
            this.paymentId = paymentId;
            this.authorized = authorized;
            this.authCode = authCode;
            this.reason = reason;
        }
    }

    // Settlement Completed
    public static class PaymentSettled implements Serializable {
        public String paymentId;
        public String settlementId;
        public String status; // COMPLETED, FAILED, PENDING
        public String transactionId;
        public LocalDateTime settledAt;

        public PaymentSettled() {}
        public PaymentSettled(String paymentId, String settlementId, String status, String transactionId) {
            this.paymentId = paymentId;
            this.settlementId = settlementId;
            this.status = status;
            this.transactionId = transactionId;
            this.settledAt = LocalDateTime.now();
        }
    }
}
