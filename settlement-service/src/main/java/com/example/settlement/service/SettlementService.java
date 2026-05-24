package com.example.settlement.service;

import com.example.common.exception.PaymentException;
import com.example.common.exception.PaymentNotFoundException;
import com.example.settlement.model.Settlement;
import com.example.settlement.repository.SettlementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;

    public SettlementService(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    /**
     * Initiate settlement for an authorized payment
     */
    public Settlement initiateSettlement(String paymentId, BigDecimal amount) {
        try {
            // Validate input
            if (paymentId == null || paymentId.isEmpty()) {
                throw new PaymentException("INVALID_SETTLEMENT_REQUEST", 
                    "Payment ID is required for settlement", 400);
            }

            // Check if settlement already exists for this payment
            var existing = settlementRepository.findByPaymentId(paymentId);
            if (existing.isPresent()) {
                System.out.println("Settlement already exists for payment: " + paymentId);
                return existing.get();
            }

            Settlement settlement = new Settlement(paymentId, amount);
            System.out.println("Settlement initiated for payment: " + paymentId);
            return settlementRepository.save(settlement);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("SETTLEMENT_INITIATION_FAILED", 
                "Failed to initiate settlement: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Complete settlement (success)
     */
    public Settlement completeSettlement(String paymentId) {
        try {
            var settlement = settlementRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            settlement.setStatus("COMPLETED");
            settlement.setTransactionId("TXN_" + UUID.randomUUID().toString());
            settlement.setSettledAt(LocalDateTime.now());

            Settlement saved = settlementRepository.save(settlement);
            System.out.println("Settlement completed for payment: " + paymentId);
            return saved;
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("SETTLEMENT_COMPLETION_FAILED", 
                "Failed to complete settlement for payment " + paymentId + ": " + e.getMessage(), 500, e);
        }
    }

    /**
     * Fail settlement
     */
    public Settlement failSettlement(String paymentId, String reason) {
        try {
            var settlement = settlementRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

            settlement.setStatus("FAILED");
            settlement.setSettledAt(LocalDateTime.now());

            Settlement saved = settlementRepository.save(settlement);
            System.out.println("Settlement failed for payment: " + paymentId + " Reason: " + reason);
            return saved;
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("SETTLEMENT_FAILURE_HANDLING_FAILED", 
                "Failed to mark settlement as failed: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Get settlement with error handling
     */
    public Settlement getSettlement(String paymentId) {
        try {
            return settlementRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("SETTLEMENT_RETRIEVAL_FAILED", 
                "Failed to retrieve settlement: " + e.getMessage(), 500, e);
        }
    }
}
