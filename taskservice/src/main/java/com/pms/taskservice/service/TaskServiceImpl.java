package com.pms.taskservice.service;

import com.pms.taskservice.dto.*;
import com.pms.taskservice.entity.*;
import com.pms.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    public TaskResponseDTO createTask(TaskRequestDTO request) {

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .assignedTo(request.getAssignedTo())
                .status(TaskStatus.TODO)
                .build();

        Task saved = taskRepository.save(task);

        log.info("Task created with ID: {}", saved.getId());

        return mapToDTO(saved);
    }

    @Override
    public List<TaskResponseDTO> getTasksByProject(Long projectId) {

        List<Task> tasks = taskRepository.findByProjectId(projectId);

        return tasks.stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public TaskResponseDTO updateStatus(Long taskId, UpdateTaskStatusDTO request) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        TaskStatus status;
        try {
            status = TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status value");
        }

        task.setStatus(status);

        Task updated = taskRepository.save(task);

        log.info("Task {} updated to status {}", taskId, status);

        return mapToDTO(updated);
    }

    private TaskResponseDTO mapToDTO(Task task) {
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .projectId(task.getProjectId())
                .assignedTo(task.getAssignedTo())
                .status(task.getStatus().name())
                .createdAt(task.getCreatedAt() != null ?
                        task.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .updatedAt(task.getUpdatedAt() != null ?
                        task.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .build();
    }
}