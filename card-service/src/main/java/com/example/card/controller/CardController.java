package com.example.card.controller;

import com.example.common.events.PaymentEvents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/cards")
public class CardController {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final com.example.card.service.CardService cardService;

    public CardController(com.example.card.service.CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<?> addCard(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String number = body.get("number");
        // persist via service
        com.example.card.model.Card card = cardService.createCard(userId, number);

        // publish Kafka event
        PaymentEvents.CardAdded event = new PaymentEvents.CardAdded(card.getId(), userId, card.getMaskedNumber());
        kafkaTemplate.send("card-added", card.getId(), event);

        return ResponseEntity.ok(Map.of("cardId", card.getId(), "maskedNumber", card.getMaskedNumber()));
    }

    @GetMapping
    public ResponseEntity<?> listCards() {
        return ResponseEntity.ok(cardService.listCards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCard(@PathVariable String id) {
        return cardService.getCard(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCard(@PathVariable String id, @RequestBody Map<String, String> body) {
        String number = body.get("number");
        try {
            com.example.card.model.Card updated = cardService.updateCard(id, number);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCard(@PathVariable String id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Tokenize a card for secure payment processing
     * Request body: {"userId": "user123", "cardNumber": "4532-1234-5678-9010", "expiryDate": "12/25", "cvv": "123"}
     * Response: {"token": "token_xyz123", "last4": "9010"}
     */
    @PostMapping("/tokenize")
    public ResponseEntity<?> tokenizeCard(@RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String cardNumber = body.get("cardNumber");
            String expiryDate = body.get("expiryDate");
            String cvv = body.get("cvv");

            if (userId == null || cardNumber == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId and cardNumber are required"));
            }

            // Tokenize the card (sensitive data not stored)
            String token = cardService.tokenizeCard(userId, cardNumber, expiryDate, cvv);
            String last4 = cardNumber.replaceAll("[^0-9]", "");
            if (last4.length() > 4) {
                last4 = last4.substring(last4.length() - 4);
            }

            return ResponseEntity.ok(Map.of(
                "token", token,
                "last4", last4,
                "message", "Card tokenized successfully. Use the token for payments."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
