package com.example.card.service;

import com.example.card.model.Card;
import com.example.card.repository.CardRepository;
import com.example.common.exception.CardTokenizationException;
import com.example.common.exception.PaymentException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CardService {

    private final CardRepository repository;

    public CardService(CardRepository repository) {
        this.repository = repository;
    }

    public Card createCard(String userId, String number) {
        try {
            if (userId == null || userId.isEmpty()) {
                throw new PaymentException("INVALID_CARD_REQUEST", "User ID is required", 400);
            }
            if (number == null || number.isEmpty()) {
                throw new PaymentException("INVALID_CARD_REQUEST", "Card number is required", 400);
            }

            String masked = mask(number);
            String id = UUID.randomUUID().toString();
            Card card = new Card(id, userId, masked);
            return repository.save(card);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("CARD_CREATION_ERROR", 
                "Failed to create card: " + e.getMessage(), 500, e);
        }
    }

    public java.util.List<Card> listCards() {
        try {
            return repository.findAll();
        } catch (Exception e) {
            throw new PaymentException("CARD_LIST_ERROR", 
                "Failed to list cards: " + e.getMessage(), 500, e);
        }
    }

    public java.util.Optional<Card> getCard(String id) {
        try {
            if (id == null || id.isEmpty()) {
                throw new PaymentException("INVALID_CARD_REQUEST", "Card ID is required", 400);
            }
            return repository.findById(id);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("CARD_RETRIEVAL_ERROR", 
                "Failed to retrieve card: " + e.getMessage(), 500, e);
        }
    }

    public Card updateCard(String id, String number) {
        try {
            if (id == null || id.isEmpty()) {
                throw new PaymentException("INVALID_CARD_REQUEST", "Card ID is required", 400);
            }
            if (number == null || number.isEmpty()) {
                throw new PaymentException("INVALID_CARD_REQUEST", "Card number is required", 400);
            }

            return repository.findById(id).map(existing -> {
                existing.setMaskedNumber(mask(number));
                return repository.save(existing);
            }).orElseThrow(() -> new PaymentException("CARD_NOT_FOUND", 
                "Card not found: " + id, 404));
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("CARD_UPDATE_ERROR", 
                "Failed to update card: " + e.getMessage(), 500, e);
        }
    }

    public void deleteCard(String id) {
        try {
            if (id == null || id.isEmpty()) {
                throw new PaymentException("INVALID_CARD_REQUEST", "Card ID is required", 400);
            }
            repository.deleteById(id);
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("CARD_DELETION_ERROR", 
                "Failed to delete card: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Tokenize card data for secure payment processing
     * Raw card data is NOT stored - only a token is generated
     * In production, integrate with a Payment Processor (Stripe, Square, etc.)
     */
    public String tokenizeCard(String userId, String cardNumber, String expiryDate, String cvv) {
        try {
            // Validate card data
            if (cardNumber == null || cardNumber.isEmpty()) {
                throw new CardTokenizationException("Card number is required");
            }
            if (userId == null || userId.isEmpty()) {
                throw new CardTokenizationException("User ID is required");
            }

            // Validate card format (basic check)
            String sanitized = cardNumber.replaceAll("[^0-9]", "");
            if (sanitized.length() < 13 || sanitized.length() > 19) {
                throw new CardTokenizationException("Card number must be between 13-19 digits");
            }

            // In production: send to payment processor (Stripe, Square, etc.) and get token back
            // For now, generate a secure token without storing sensitive data
            String token = generateSecureToken(userId, cardNumber);

            // Log masked card info (for audit purposes only)
            String masked = mask(cardNumber);
            System.out.println("Card tokenized for user " + userId + " - masked: " + masked);

            return token;
        } catch (CardTokenizationException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("CARD_TOKENIZATION_ERROR", 
                "Failed to tokenize card: " + e.getMessage(), 500, e);
        }
    }

    /**
     * Generate a secure token for the card (simulates payment processor tokenization)
     * In production, this would call Stripe, Square, or similar
     */
    private String generateSecureToken(String userId, String cardNumber) {
        // Create a hash of the card number + user ID + random salt
        // This simulates what a payment processor would do
        String hash = Integer.toHexString((userId + cardNumber + System.nanoTime()).hashCode());
        return "token_" + hash + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String mask(String number) {
        if (number == null || number.isEmpty()) {
            return "****";
        }
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return "****";
        }
        return "**** **** **** " + digits.substring(digits.length() - 4);
    }
}
