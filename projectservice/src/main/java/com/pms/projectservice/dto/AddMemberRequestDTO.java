package com.pms.projectservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddMemberRequestDTO {

    @NotBlank(message = "UserId (email) is required")
    @Email(message = "Invalid email format")
    private String userId;

    @Pattern(
        regexp = "ADMIN|MEMBER|VIEWER",
        message = "Role must be ADMIN, MEMBER or VIEWER"
    )
    private String role;
}