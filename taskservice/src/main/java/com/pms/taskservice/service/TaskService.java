package com.pms.taskservice.service;

import com.pms.taskservice.dto.*;

import org.springframework.data.domain.Page;


public interface TaskService {

    TaskResponseDTO createTask(TaskRequestDTO request);

    Page<TaskResponseDTO> getTasksByProject(
                Long projectId,
                int page,
                int size,
                String status,
                String sortBY,
                String direction
            );

    TaskResponseDTO updateStatus(Long taskId, UpdateTaskStatusDTO request);
}