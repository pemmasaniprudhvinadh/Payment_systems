package com.example.card.service;

import com.example.card.model.Card;
import com.example.card.repository.CardRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CardService {

    private final CardRepository repository;

    public CardService(CardRepository repository) {
        this.repository = repository;
    }

    public Card createCard(String userId, String number) {
        String masked = mask(number);
        String id = UUID.randomUUID().toString();
        Card card = new Card(id, userId, masked);
        return repository.save(card);
    }

    public java.util.List<Card> listCards() {
        return repository.findAll();
    }

    public java.util.Optional<Card> getCard(String id) {
        return repository.findById(id);
    }

    public Card updateCard(String id, String number) {
        return repository.findById(id).map(existing -> {
            existing.setMaskedNumber(mask(number));
            return repository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Card not found: " + id));
    }

    public void deleteCard(String id) {
        repository.deleteById(id);
    }

    /**
     * Tokenize card data for secure payment processing
     * Raw card data is NOT stored - only a token is generated
     * In production, integrate with a Payment Processor (Stripe, Square, etc.)
     */
    public String tokenizeCard(String userId, String cardNumber, String expiryDate, String cvv) {
        // Validate card data
        if (cardNumber == null || cardNumber.isEmpty()) {
            throw new IllegalArgumentException("Card number is required");
        }

        // In production: send to payment processor (Stripe, Square, etc.) and get token back
        // For now, generate a secure token without storing sensitive data
        String token = generateSecureToken(userId, cardNumber);

        // Log masked card info (for audit purposes only)
        String masked = mask(cardNumber);
        System.out.println("Card tokenized for user " + userId + " - masked: " + masked);

        return token;
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
        if (number == null || number.length() < 4) return "****";
        return "**** **** **** " + number.replaceAll("[^0-9]", "").substring(Math.max(0, number.length() - 4));
    }
}
