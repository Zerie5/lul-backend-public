package com.lul.dto.base;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class DeviceInfoBase {
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @NotBlank(message = "Device name is required")
    private String deviceName;
    
    @NotBlank(message = "OS is required")
    private String os;
    
    private String ipAddress;
} 