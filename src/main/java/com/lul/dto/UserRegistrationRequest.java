package com.lul.dto;

import com.lul.dto.base.ProfileBase;
import com.lul.dto.base.DeviceInfoBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;


@Data
@EqualsAndHashCode(callSuper = true)
public class UserRegistrationRequest extends ProfileBase {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
             message = "Password must contain at least one digit, lowercase, and uppercase letter")
    private String password;
    
    private DeviceInfoBase deviceInfo = new DeviceInfoBase() {{
        setDeviceId("Unknown");
        setDeviceName("Unknown");
        setOs("Unknown");
    }};
    
    // Optional fields already in ProfileBase:
    // private String gender;
    // private LocalDate dateOfBirth;
    private String referredBy;
    
    @NotNull(message = "You must agree to the terms and conditions")
    private Boolean termsAgreed = false;
} 