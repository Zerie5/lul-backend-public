package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(name = "transaction_history", schema = "wallet")
@Data
public class TransactionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;
    
    @Column(name = "sender_id", nullable = false)
    private Integer senderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private User sender;
    
    @Column(name = "receiver_id")
    private Integer receiverId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", insertable = false, updatable = false)
    private User receiver;
    
    @Column(name = "sender_wallet_id")
    private Integer senderWalletId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet_id", insertable = false, updatable = false)
    private UserWallet senderWallet;
    
    @Column(name = "receiver_wallet_id")
    private Integer receiverWalletId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet_id", insertable = false, updatable = false)
    private UserWallet receiverWallet;
    
    @Column(name = "transaction_type_id", nullable = false)
    private Integer transactionTypeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type_id", insertable = false, updatable = false)
    private TransactionType transactionType;
    
    @Column(name = "transaction_status_id", nullable = false)
    private Integer transactionStatusId = 1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_status_id", insertable = false, updatable = false)
    private TransactionStatus transactionStatus;
    
    @Column(name = "disbursement_stage_id")
    private Integer disbursementStageId = 1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursement_stage_id", insertable = false, updatable = false)
    private DisbursementStage disbursementStage;
    
    @Column(name = "exchange_rate_id")
    private Integer exchangeRateId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_rate_id", insertable = false, updatable = false)
    private ExchangeRate exchangeRate;
    
    @Column(name = "batch_id")
    private Integer batchId;
    
    @Column(name = "transacted_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal transactedValue;
    
    @Column(name = "fee", precision = 15, scale = 2)
    private BigDecimal fee;
    
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "currency", length = 10)
    private String currency;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "additional_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> additionalData;
    
    @Column(name = "is_reversal")
    private Boolean isReversal = false;
    
    @Column(name = "original_transaction_id")
    private Long originalTransactionId;
    
    @Column(name = "reversal_reason")
    private String reversalReason;
    
    @Column(name = "reversed_by")
    private Integer reversedBy;
    
    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 