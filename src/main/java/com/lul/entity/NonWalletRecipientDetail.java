package com.lul.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity to store recipient details for non-wallet transfers.
 * This entity captures all necessary information about recipients who don't have a wallet
 * in the system but are receiving funds through a non-wallet transfer.
 */
@Entity
@Table(name = "non_wallet_recipient_details", schema = "wallet")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonWalletRecipientDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_history_id", nullable = false)
    private Long transactionHistoryId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "id_document_type", nullable = false, length = 50)
    private String idDocumentType;

    @Column(name = "id_number", nullable = false, length = 50)
    private String idNumber;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "country", nullable = false, length = 50)
    private String country;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Column(name = "disbursement_stage_id")
    private Integer disbursementStageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 