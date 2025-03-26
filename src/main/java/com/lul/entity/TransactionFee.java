package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_fees", schema = "wallet")
@Data
public class TransactionFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    
    @Column(name = "fee_type_id", nullable = false)
    private Integer feeTypeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_type_id", insertable = false, updatable = false)
    private FeeType feeType;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 10, nullable = false)
    private String currency;
    
    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;
    
    @Column(name = "fixed_amount", precision = 15, scale = 2)
    private BigDecimal fixedAmount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 