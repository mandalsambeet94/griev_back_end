package com.grievance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "user name is required")
    private String name;

    @NotBlank(message = "Password is required")
    private String password;
}