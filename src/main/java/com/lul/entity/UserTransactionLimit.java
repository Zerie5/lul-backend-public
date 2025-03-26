package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_transaction_limits", schema = "wallet")
@Data
public class UserTransactionLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @Column(name = "kyc_level_id", nullable = false)
    private Integer kycLevelId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kyc_level_id", insertable = false, updatable = false)
    private KycLevel kycLevel;
    
    @Column(name = "daily_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal dailyLimit;
    
    @Column(name = "daily_used", precision = 15, scale = 2)
    private BigDecimal dailyUsed = BigDecimal.ZERO;
    
    @Column(name = "monthly_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal monthlyLimit;
    
    @Column(name = "monthly_used", precision = 15, scale = 2)
    private BigDecimal monthlyUsed = BigDecimal.ZERO;
    
    @Column(name = "annual_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal annualLimit;
    
    @Column(name = "annual_used", precision = 15, scale = 2)
    private BigDecimal annualUsed = BigDecimal.ZERO;
    
    @Column(name = "last_reset_daily")
    private LocalDateTime lastResetDaily;
    
    @Column(name = "last_reset_monthly")
    private LocalDateTime lastResetMonthly;
    
    @Column(name = "last_reset_annual")
    private LocalDateTime lastResetAnnual;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
