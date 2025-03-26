package com.lul.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;


@Entity
@Table(name = "user_profiles", schema = "auth")
@Data
public class UserProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "first_name", length = 250)
    private String firstName;

    @Column(name = "last_name", length = 250)
    private String lastName;

    @Column(name = "whatsapp_number", length = 30)
    private String whatsappNumber;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "kyc_level")
    private Integer kycLevel = 1;

    @Column(name = "referred_by", length = 250)
    private String referredBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "state", length = 250)
    private String state;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Override
    public String toString() {
        return "UserProfile{" +
            "id=" + userId +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            // Don't include user to avoid circular reference
            '}';
    }
} 