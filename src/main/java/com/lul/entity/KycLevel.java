package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_levels", schema = "wallet")
@Data
public class KycLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "name", length = 50, nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "daily_limit", precision = 15, scale = 2)
    private BigDecimal dailyLimit;
    
    @Column(name = "monthly_limit", precision = 15, scale = 2)
    private BigDecimal monthlyLimit;
    
    @Column(name = "annual_limit", precision = 15, scale = 2)
    private BigDecimal annualLimit;
    
    @Column(name = "max_transaction_amount", precision = 15, scale = 2)
    private BigDecimal maxTransactionAmount;
    
    @Column(name = "min_transaction_amount", precision = 15, scale = 2)
    private BigDecimal minTransactionAmount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
