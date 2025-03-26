package com.lul.repository;

import com.lul.entity.DisbursementStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing disbursement stages
 */
@Repository
public interface DisbursementStageRepository extends JpaRepository<DisbursementStage, Integer> {
    
    /**
     * Find disbursement stage by name
     * 
     * @param stageName The stage name
     * @return The disbursement stage
     */
    Optional<DisbursementStage> findByStageName(String stageName);
} 