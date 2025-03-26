package com.lul.repository;

import com.lul.entity.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpStatusRepository extends JpaRepository<OtpStatus, Integer> {
    Optional<OtpStatus> findByStatusName(String statusName);
} 