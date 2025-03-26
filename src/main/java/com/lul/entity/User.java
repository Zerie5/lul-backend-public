package com.lul.entity;

import jakarta.persistence.*;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users", schema = "auth")
@Data
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;
    
    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;
    
    @Column(name = "phone_number", length = 30, nullable = false)
    private String phoneNumber;
    
    @Column(name = "password_hash", length = 500)
    private String passwordHash;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "status_id", nullable = false)
    private Integer statusId;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;
    
    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;
    
    @Column(name = "oauth_provider_id", length = 255)
    private String oauthProviderId;
    
    @Column(name = "is_social_login")
    private Boolean isSocialLogin = false;
    
    @Column(name = "user_work_id", length = 10, unique = true)
    private String userWorkId;
    
    @Column(name = "pin_hash", length = 500)
    private String pinHash;
    
    @Column(name = "register_status")
    private Integer registerStatus = 1;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile profile;

 

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFirstName() {
        return profile != null ? profile.getFirstName() : null;
    }

    public String getLastName() {
        return profile != null ? profile.getLastName() : null;
    }

    public Long getId() {
        return id;
    }

    public String getUserWorkId() {
        return userWorkId;
    }

    public void setUserWorkId(String userWorkId) {
        this.userWorkId = userWorkId;
    }

    @Override
    public String toString() {
        return "User{" +
            "id=" + id +
            ", email='" + email + '\'' +
            ", username='" + username + '\'' +
            // Don't include profile to avoid circular reference
            '}';
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();  // Or return actual roles if you have them
    }

    

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPassword() {
        return passwordHash;  // Return passwordHash since we don't store plain password
    }
} 