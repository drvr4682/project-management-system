package com.pms.taskservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be less than 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @Pattern(
        regexp = "TODO|IN_PROGRESS|DONE|BLOCKED",
        message = "Status must be TODO, IN_PROGRESS, DONE or BLOCKED"
    )
    private String status;

    @Pattern(
        regexp = "LOW|MEDIUM|HIGH|CRITICAL",
        message = "Priority must be LOW, MEDIUM, HIGH or CRITICAL"
    )
    private String priority;

    private LocalDateTime dueDate;

    @NotNull(message = "Project ID is required")
    private Long projectId;
}
