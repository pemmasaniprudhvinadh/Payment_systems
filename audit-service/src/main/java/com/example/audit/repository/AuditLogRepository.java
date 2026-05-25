package com.example.audit.repository;

import com.example.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPaymentId(String paymentId);
    List<AuditLog> findByUserId(String userId);
    List<AuditLog> findByEventType(String eventType);
}
