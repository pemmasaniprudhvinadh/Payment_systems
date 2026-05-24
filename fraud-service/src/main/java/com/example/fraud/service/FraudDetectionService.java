package com.example.fraud.service;

import com.example.common.exception.PaymentException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class FraudDetectionService {

    /**
     * Performs fraud detection on a payment
     * Returns a risk score (0.0 - 1.0) where 1.0 is highest fraud risk
     */
    public FraudAnalysis analyzeFraud(String paymentId, String userId, String tokenizedCardId, 
                                      BigDecimal amount, String currency) {
        try {
            // Validate inputs
            if (paymentId == null || paymentId.isEmpty()) {
                throw new PaymentException("INVALID_FRAUD_REQUEST", 
                    "Payment ID is required for fraud analysis", 400);
            }
            if (tokenizedCardId == null || tokenizedCardId.isEmpty()) {
                throw new PaymentException("INVALID_FRAUD_REQUEST", 
                    "Card token is required for fraud analysis", 400);
            }
            if (amount == null) {
                throw new PaymentException("INVALID_FRAUD_REQUEST", 
                    "Amount is required for fraud analysis", 400);
            }

            double riskScore = 0.0;
            String reason = "Payment passed fraud checks";
            boolean isFraudulent = false;

            // Check 1: Unusual amount
            if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
                riskScore += 0.2;
                reason = "High transaction amount";
            }

            // Check 2: Token validation
            if (!tokenizedCardId.startsWith("token_")) {
                riskScore += 0.3;
                reason = "Invalid card token format";
                isFraudulent = true;
            }

            // Check 3: Random risk factor (in production, use ML model)
            double randomRisk = Math.random() * 0.1;
            riskScore += randomRisk;

            // Determine if fraudulent
            if (riskScore > 0.5) {
                isFraudulent = true;
            }

            return new FraudAnalysis(isFraudulent, reason, riskScore);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("FRAUD_ANALYSIS_ERROR", 
                "Error during fraud analysis: " + e.getMessage(), 500, e);
        }
    }

    public static class FraudAnalysis {
        public boolean isFraudulent;
        public String reason;
        public double riskScore;

        public FraudAnalysis(boolean isFraudulent, String reason, double riskScore) {
            this.isFraudulent = isFraudulent;
            this.reason = reason;
            this.riskScore = riskScore;
        }
    }
}
