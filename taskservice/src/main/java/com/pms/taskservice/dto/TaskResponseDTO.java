package com.pms.taskservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskResponseDTO {

    private Long id;
    private String title;
    private String description;
    private Long projectId;
    private String assignedTo;
    private String status;
    private Long createdAt;
    private Long updatedAt;
}