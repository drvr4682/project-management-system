package com.pms.taskservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long dueDate;
    private Long projectId;
    private String createdBy;
    private String assignedTo;
    private Long createdAt;
    private Long updatedAt;
}
