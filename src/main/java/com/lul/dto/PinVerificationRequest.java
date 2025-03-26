package com.lul.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class PinVerificationRequest {
    @NotNull(message = "PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits")
    private String pin;
} 