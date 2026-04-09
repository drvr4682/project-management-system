package com.pms.taskservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTaskStatusDTO {

    @NotBlank(message = "Status is required")
    private String status;
}