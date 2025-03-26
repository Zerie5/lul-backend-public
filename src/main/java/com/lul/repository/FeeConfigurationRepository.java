package com.lul.repository;

import com.lul.entity.FeeConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeeConfigurationRepository extends JpaRepository<FeeConfiguration, Integer> {
    Optional<FeeConfiguration> findByFeeTypeIdAndIsActive(Integer feeTypeId, Boolean isActive);
} 