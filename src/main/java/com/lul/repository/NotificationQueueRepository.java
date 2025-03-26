package com.lul.repository;

import com.lul.entity.NotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, Integer> {
    List<NotificationQueue> findByUserId(Integer userId);
    
    List<NotificationQueue> findByTransactionId(Long transactionId);
    
    List<NotificationQueue> findByStatusAndNextRetryAtBefore(String status, LocalDateTime nextRetryAt);
    
    List<NotificationQueue> findByStatus(String status);
} 