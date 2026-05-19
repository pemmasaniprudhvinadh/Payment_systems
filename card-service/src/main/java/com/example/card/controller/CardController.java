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
}
