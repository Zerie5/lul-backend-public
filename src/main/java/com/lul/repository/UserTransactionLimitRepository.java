package com.lul.repository;

import com.lul.entity.UserTransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTransactionLimitRepository extends JpaRepository<UserTransactionLimit, Integer> {
    Optional<UserTransactionLimit> findByUserId(Integer userId);
} 