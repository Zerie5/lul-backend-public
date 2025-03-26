package com.lul.repository;

import com.lul.entity.TransactionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionAuditLogRepository extends JpaRepository<TransactionAuditLog, Integer> {
    List<TransactionAuditLog> findByTransactionId(Long transactionId);
    
    List<TransactionAuditLog> findByPerformedBy(Integer performedBy);
} 