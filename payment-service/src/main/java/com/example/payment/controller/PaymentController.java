package com.example.payment.controller;

import com.example.common.exception.*;
import com.example.payment.model.Payment;
import com.example.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Initiate a new payment request
     * 
     * Request body:
     * {
     *   "userId": "user123",
     *   "tokenizedCardId": "token_xyz",
     *   "amount": 99.99,
     *   "currency": "USD",
     *   "merchantId": "merchant123",
     *   "idempotencyKey": "unique-key-123"  // optional, for idempotency
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> initiatePayment(@RequestBody Map<String, Object> body) {
        try {
            String userId = (String) body.get("userId");
            String tokenizedCardId = (String) body.get("tokenizedCardId");
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String currency = (String) body.getOrDefault("currency", "USD");
            String merchantId = (String) body.get("merchantId");
            String idempotencyKey = (String) body.get("idempotencyKey");

            Payment payment = paymentService.initiatePayment(userId, tokenizedCardId, amount, 
                                                            currency, merchantId, idempotencyKey);

            return ResponseEntity.accepted().body(Map.of(
                "paymentId", payment.getId(),
                "status", payment.getStatus().toString(),
                "idempotencyKey", payment.getIdempotencyKey(),
                "message", "Payment initiated. Awaiting fraud check..."
            ));
        } catch (PaymentValidationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getErrorCode(),
                "message", e.getMessage()
            ));
        } catch (PaymentException e) {
            return ResponseEntity.status(e.getHttpStatusCode()).body(Map.of(
                "error", e.getErrorCode(),
                "message", e.getMessage()
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_AMOUNT",
                "message", "Invalid amount format. Expected a numeric value."
            ));
        } catch (NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "MISSING_REQUIRED_FIELD",
                "message", "Missing required field: " + e.getMessage()
            ));
        } catch (Exception e) {
            System.err.println("Unexpected error during payment initiation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "PAYMENT_INITIATION_ERROR",
                "message", "An unexpected error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Get payment status
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String paymentId) {
        try {
            if (paymentId == null || paymentId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_PAYMENT_ID",
                    "message", "Payment ID is required"
                ));
            }

            Payment payment = paymentService.getPayment(paymentId);
            return ResponseEntity.ok(Map.of(
                "paymentId", payment.getId(),
                "status", payment.getStatus().toString(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency(),
                "fraudCheckStatus", payment.getFraudCheckStatus(),
                "fraudRiskScore", payment.getFraudRiskScore(),
                "authCode", payment.getAuthCode(),
                "transactionId", payment.getTransactionId(),
                "createdAt", payment.getCreatedAt(),
                "updatedAt", payment.getUpdatedAt()
            ));
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", e.getErrorCode(),
                "message", e.getMessage()
            ));
        } catch (PaymentException e) {
            return ResponseEntity.status(e.getHttpStatusCode()).body(Map.of(
                "error", e.getErrorCode(),
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            System.err.println("Unexpected error retrieving payment: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "PAYMENT_RETRIEVAL_ERROR",
                "message", "An unexpected error occurred"
            ));
        }
    }
}
