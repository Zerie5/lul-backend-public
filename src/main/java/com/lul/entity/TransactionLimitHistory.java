package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_limit_history", schema = "wallet")
@Data
public class TransactionLimitHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 10, nullable = false)
    private String currency;
    
    @Column(name = "limit_type", length = 20, nullable = false)
    private String limitType;
    
    @Column(name = "previous_used", precision = 15, scale = 2, nullable = false)
    private BigDecimal previousUsed;
    
    @Column(name = "new_used", precision = 15, scale = 2, nullable = false)
    private BigDecimal newUsed;
    
    @Column(name = "limit_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal limitValue;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
