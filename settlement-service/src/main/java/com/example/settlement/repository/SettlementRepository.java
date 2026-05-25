package com.example.settlement.repository;

import com.example.settlement.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, String> {
    Optional<Settlement> findByPaymentId(String paymentId);
}
