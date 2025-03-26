package com.lul.repository;

import com.lul.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeeTypeRepository extends JpaRepository<FeeType, Integer> {
    Optional<FeeType> findByName(String name);
} 