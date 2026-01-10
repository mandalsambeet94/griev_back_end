package com.grievance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "Contact number is required")
    private String contact;

    @NotBlank(message = "Password is required")
    private String password;
}