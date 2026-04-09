package com.pms.taskservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "ProjectId is required")
    private Long projectId;

    @NotBlank(message = "Assigned user is required")
    private String assignedTo;
}