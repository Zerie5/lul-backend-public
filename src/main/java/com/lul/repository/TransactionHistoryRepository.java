package com.lul.repository;

import com.lul.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Integer> {
    Optional<TransactionHistory> findByTransactionId(Long transactionId);
    
    List<TransactionHistory> findBySenderId(Integer senderId);
    
    List<TransactionHistory> findByReceiverId(Integer receiverId);
    
    List<TransactionHistory> findBySenderWalletId(Integer senderWalletId);
    
    List<TransactionHistory> findByReceiverWalletId(Integer receiverWalletId);
    
    /**
     * Count transactions between the given dates
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Count of transactions
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Sum the transacted value of all transactions
     * 
     * @return Total transacted value
     */
    @Query("SELECT COALESCE(SUM(t.transactedValue), 0) FROM TransactionHistory t")
    BigDecimal sumTransactedValue();
    
    /**
     * Sum the transacted value of transactions between the given dates
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Total transacted value
     */
    @Query("SELECT COALESCE(SUM(t.transactedValue), 0) FROM TransactionHistory t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTransactedValueBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Sum the fee of all transactions
     * 
     * @return Total fees
     */
    @Query("SELECT COALESCE(SUM(t.fee), 0) FROM TransactionHistory t")
    BigDecimal sumFees();
    
    /**
     * Sum the fee of transactions between the given dates
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Total fees
     */
    @Query("SELECT COALESCE(SUM(t.fee), 0) FROM TransactionHistory t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumFeesBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 