package com.lul.repository;

import com.lul.entity.TransactionFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionFeeRepository extends JpaRepository<TransactionFee, Integer> {
    Optional<TransactionFee> findByTransactionId(Long transactionId);
} 