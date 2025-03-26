package com.lul.repository;

import com.lul.entity.TransactionLimitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLimitHistoryRepository extends JpaRepository<TransactionLimitHistory, Integer> {
    List<TransactionLimitHistory> findByUserId(Integer userId);
    
    List<TransactionLimitHistory> findByTransactionId(Long transactionId);
} 