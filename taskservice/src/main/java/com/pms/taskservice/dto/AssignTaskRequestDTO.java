package com.pms.taskservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignTaskRequestDTO {

    @NotBlank(message = "Assignee user ID (email) is required")
    @Email(message = "Invalid email format")
    private String assigneeId;
}
