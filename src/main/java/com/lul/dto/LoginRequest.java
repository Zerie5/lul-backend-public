package com.lul.dto;

import com.lul.dto.base.DeviceInfoBase;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotNull(message = "Device information is required")
    private DeviceInfoBase deviceInfo;
    
    // FCM token is optional during login
    private String fcmToken;
} 