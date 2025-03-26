package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_queue", schema = "notification")
@Data
public class NotificationQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "notification_type_id", nullable = false)
    private Integer notificationTypeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_type_id", insertable = false, updatable = false)
    private NotificationType notificationType;
    
    @Column(name = "channel_id", nullable = false)
    private Integer channelId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", insertable = false, updatable = false)
    private Channel channel;
    
    @Column(name = "subject", length = 255)
    private String subject;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "transaction_id")
    private Long transactionId;
    
    @Column(name = "reference_id")
    private Long referenceId;
    
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    
    @Column(name = "status", length = 20, nullable = false)
    private String status;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
