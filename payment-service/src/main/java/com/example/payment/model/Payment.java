package com.example.payment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private String id;
    
    @Column(unique = true)
    private String idempotencyKey;
    
    private String userId;
    private String tokenizedCardId;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private String fraudCheckStatus;
    private Double fraudRiskScore;
    private String authCode;
    private String settlementId;
    private String transactionId;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Payment() {}

    public Payment(String id, String idempotencyKey, String userId, String tokenizedCardId, 
                   BigDecimal amount, String currency, String merchantId) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.tokenizedCardId = tokenizedCardId;
        this.amount = amount;
        this.currency = currency;
        this.merchantId = merchantId;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTokenizedCardId() { return tokenizedCardId; }
    public void setTokenizedCardId(String tokenizedCardId) { this.tokenizedCardId = tokenizedCardId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getFraudCheckStatus() { return fraudCheckStatus; }
    public void setFraudCheckStatus(String fraudCheckStatus) { this.fraudCheckStatus = fraudCheckStatus; }

    public Double getFraudRiskScore() { return fraudRiskScore; }
    public void setFraudRiskScore(Double fraudRiskScore) { this.fraudRiskScore = fraudRiskScore; }

    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }

    public String getSettlementId() { return settlementId; }
    public void setSettlementId(String settlementId) { this.settlementId = settlementId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum PaymentStatus {
        PENDING, FRAUD_CHECK_PENDING, FRAUD_DETECTED, AUTHORIZED, AUTHORIZATION_FAILED, 
        SETTLEMENT_PENDING, SETTLED, SETTLEMENT_FAILED
    }
}
