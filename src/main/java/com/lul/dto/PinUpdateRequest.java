package com.lul.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PinUpdateRequest {
    @NotNull(message = "PIN is required")
    @Size(min = 4, max = 4, message = "PIN must be 4 digits")
    private String pin;
} 