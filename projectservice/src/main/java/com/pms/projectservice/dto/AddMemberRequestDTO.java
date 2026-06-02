package com.pms.projectservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddMemberRequestDTO {

    @NotBlank(message = "UserId is required")
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "UserId must be a valid UUID"
    )
    private String userId;

    @Pattern(
        regexp = "ADMIN|MEMBER|VIEWER",
        message = "Role must be ADMIN, MEMBER or VIEWER"
    )
    private String role;
}