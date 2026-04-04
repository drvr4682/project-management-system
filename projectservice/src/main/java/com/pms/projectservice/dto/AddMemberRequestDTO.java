package com.pms.projectservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMemberRequestDTO {

    @NotBlank(message = "UserId (email) is required")
    @Email(message = "Invalid email format")
    private String userId;

    @NotBlank(message = "Role is required")
    private String role; //ADMIN / MEMBER / VIEWER
}
