package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_configuration", schema = "wallet")
@Data
public class FeeConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "fee_type_id", nullable = false)
    private Integer feeTypeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_type_id", insertable = false, updatable = false)
    private FeeType feeType;
    
    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;
    
    @Column(name = "fixed_amount", precision = 15, scale = 2)
    private BigDecimal fixedAmount;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
