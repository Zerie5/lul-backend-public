package com.lul.dto.base;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class ProfileBase {
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phoneNumber;
    private String whatsappNumber;
    private String city;
    private String country;
    private String state;
    private String gender;
    private LocalDate dateOfBirth;
} 