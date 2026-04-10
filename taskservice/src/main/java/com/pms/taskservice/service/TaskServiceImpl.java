package com.pms.taskservice.service;

import com.pms.taskservice.client.AuthFeignClient;
import com.pms.taskservice.client.ProjectFeignClient;
import com.pms.taskservice.dto.*;
import com.pms.taskservice.entity.*;
import com.pms.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import feign.FeignException;
import feign.RetryableException;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final AuthFeignClient authFeignClient;
    private final ProjectFeignClient projectFeignClient;

    @Override
    public TaskResponseDTO createTask(TaskRequestDTO request) {

        //  1. Validate user exists
        try {
            String response = authFeignClient.checkUser(request.getAssignedTo());

            if (!"User exists".equalsIgnoreCase(response)) {
                throw new IllegalArgumentException("User does not exist");
            }

        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("User does not exist");

        } catch (RetryableException e) {
            throw new IllegalArgumentException("Auth service unavailable");

        } catch (FeignException e) {
            throw new IllegalArgumentException("Error calling auth service");
        }

        //  2. Validate project access (membership enforced in project service)
        try {
            projectFeignClient.getProject(request.getProjectId());

        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Project not found");

        } catch (FeignException.Forbidden e) {
            throw new RuntimeException("Access denied to project");

        } catch (RetryableException e) {
            throw new RuntimeException("Project service unavailable");

        } catch (FeignException e) {
            throw new RuntimeException("Error calling project service");
        }

        //  3. Save task
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

        // validate access
        projectFeignClient.getProject(projectId);

        return taskRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public TaskResponseDTO updateStatus(Long taskId, UpdateTaskStatusDTO request) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // validate access
        projectFeignClient.getProject(task.getProjectId());

        TaskStatus status;
        try {
            status = TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status value");
        }

        task.setStatus(status);

        Task updated = taskRepository.save(task);

        log.info("Task {} updated to {}", taskId, status);

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