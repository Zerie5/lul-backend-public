package com.lul.repository;

import com.lul.entity.OtpMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OtpMethodRepository extends JpaRepository<OtpMethod, Integer> {
    Optional<OtpMethod> findByMethodName(String methodName);
} 