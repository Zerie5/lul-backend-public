package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transaction_audit_log", schema = "wallet")
@Data
public class TransactionAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "transaction_id")
    private Long transactionId;
    
    @Column(name = "action", length = 50, nullable = false)
    private String action;
    
    @Column(name = "performed_by", nullable = false)
    private Integer performedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", insertable = false, updatable = false)
    private User performer;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "device_info")
    private String deviceInfo;
    
    @Column(name = "old_state", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Object oldState;
    
    @Column(name = "new_state", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Object newState;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
