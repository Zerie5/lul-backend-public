package com.lul.repository;

import com.lul.entity.NonWalletRecipientDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing non-wallet recipient details
 */
@Repository
public interface NonWalletRecipientDetailRepository extends JpaRepository<NonWalletRecipientDetail, Integer> {
    
    /**
     * Find recipient details by transaction history ID
     * 
     * @param transactionHistoryId The transaction history ID
     * @return The recipient details
     */
    Optional<NonWalletRecipientDetail> findByTransactionHistoryId(Integer transactionHistoryId);
    
    /**
     * Find recipient details by phone number
     * 
     * @param phoneNumber The recipient's phone number
     * @return List of recipient details
     */
    List<NonWalletRecipientDetail> findByPhoneNumber(String phoneNumber);
    
    /**
     * Find recipient details by ID number
     * 
     * @param idNumber The recipient's ID number
     * @return List of recipient details
     */
    List<NonWalletRecipientDetail> findByIdNumber(String idNumber);
} 