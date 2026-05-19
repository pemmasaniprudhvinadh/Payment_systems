package com.example.loan.kafka;

import com.example.common.events.PaymentEvents;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CardAddedListener {

    @KafkaListener(topics = "card-added", groupId = "loan-service")
    public void onCardAdded(PaymentEvents.CardAdded event) {
        // react to card added event (e.g., attach card meta to loans, trigger checks)
        System.out.println("Received CardAdded event for cardId=" + event.cardId + " userId=" + event.userId);
    }
}
