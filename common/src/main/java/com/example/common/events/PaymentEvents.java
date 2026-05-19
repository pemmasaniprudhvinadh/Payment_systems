package com.example.common.events;

import java.io.Serializable;

public class PaymentEvents {
    public static class CardAdded implements Serializable {
        public String cardId;
        public String userId;
        public String maskedNumber;

        public CardAdded() {}
        public CardAdded(String cardId, String userId, String maskedNumber) {
            this.cardId = cardId;
            this.userId = userId;
            this.maskedNumber = maskedNumber;
        }
    }
}
