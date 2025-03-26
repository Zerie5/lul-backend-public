package com.lul.repository;

import com.lul.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Integer> {
    Optional<IdempotencyKey> findByKey(String key);
} 