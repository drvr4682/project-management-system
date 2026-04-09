package com.pms.taskservice.service;

import com.pms.taskservice.dto.*;

import java.util.List;

public interface TaskService {

    TaskResponseDTO createTask(TaskRequestDTO request);

    List<TaskResponseDTO> getTasksByProject(Long projectId);

    TaskResponseDTO updateStatus(Long taskId, UpdateTaskStatusDTO request);
}