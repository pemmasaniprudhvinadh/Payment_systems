package com.example.card.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    private String id;
    private String userId;
    private String maskedNumber;

    public Card() {}

    public Card(String id, String userId, String maskedNumber) {
        this.id = id;
        this.userId = userId;
        this.maskedNumber = maskedNumber;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMaskedNumber() { return maskedNumber; }
    public void setMaskedNumber(String maskedNumber) { this.maskedNumber = maskedNumber; }
}
