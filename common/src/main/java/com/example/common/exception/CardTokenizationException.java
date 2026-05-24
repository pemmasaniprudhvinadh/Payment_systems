package com.example.common.exception;

/**
 * Thrown when card tokenization fails
 */
public class CardTokenizationException extends PaymentException {
    public CardTokenizationException(String message) {
        super("CARD_TOKENIZATION_FAILED", message, 400);
    }

    public CardTokenizationException(String message, Throwable cause) {
        super("CARD_TOKENIZATION_FAILED", message, 400, cause);
    }
}
