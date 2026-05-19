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

    private String mask(String number) {
        if (number == null || number.length() < 4) return "****";
        return "**** **** **** " + number.substring(number.length()-4);
    }
}
