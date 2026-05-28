package com.pms.authservice.dto;

import com.pms.authservice.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @jakarta.validation.constraints.Pattern(
        regexp = "^[a-zA-Z0-9_]{3,30}$",
        message = "Username must be alphanumeric and between 3 to 30 characters"
    )
    private String userName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain uppercase, lowercase, number, special character and minimum 8 characters"
    )
    private String password;
}