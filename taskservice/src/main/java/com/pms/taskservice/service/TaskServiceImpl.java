package com.pms.taskservice.service;

import com.pms.taskservice.client.AuthFeignClient;
import com.pms.taskservice.client.ProjectFeignClient;
import com.pms.taskservice.dto.ProjectResponseDTO;
import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.dto.TaskResponseDTO;
import com.pms.taskservice.dto.UpdateTaskStatusDTO;
import com.pms.taskservice.dto.UserExistsResponse;
import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskStatus;
import com.pms.taskservice.exception.AccessDeniedException;
import com.pms.taskservice.exception.ResourceNotFoundException;
import com.pms.taskservice.exception.ServiceUnavailableException;
import com.pms.taskservice.repository.TaskRepository;

import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final AuthFeignClient authFeignClient;
    private final ProjectFeignClient projectFeignClient;

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    public TaskResponseDTO createTask(TaskRequestDTO request) {
        String user = getCurrentUser();
        log.info("ACTION=CREATE_TASK_REQUEST | USER={} | PROJECT={} | ASSIGNED_TO={}",
                user, request.getProjectId(), request.getAssignedTo());

        validateUser(request.getAssignedTo());
        validateProjectAccess(request.getProjectId());
        validateProjectAdmin(request.getProjectId());

        Task saved = taskRepository.save(Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .assignedTo(request.getAssignedTo())
                .status(TaskStatus.TODO)
                .build());

        log.info("ACTION=CREATE_TASK_SUCCESS | USER={} | PROJECT={} | TASK={}",
                user, saved.getProjectId(), saved.getId());
        return mapToDTO(saved);
    }

    // ── Resilience-wrapped helper methods ─────────────────────────────────────

    @CircuitBreaker(name = "auth-service", fallbackMethod = "userValidationFallback")
    @Retry(name = "auth-service")
    public void validateUser(String email) {
        try {
            UserExistsResponse response = authFeignClient.checkUser(email);
            if (!response.isExists()) {
                throw new IllegalArgumentException("User does not exist: " + email);
            }
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("User does not exist: " + email);
        }
    }

    // Called automatically when circuit is OPEN or all retries exhausted
    public void userValidationFallback(String email, Throwable t) {
        log.error("Auth service circuit OPEN or retries exhausted for email: {}. Cause: {}", email, t.getMessage());
        throw new ServiceUnavailableException("Auth service is currently unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "project-service", fallbackMethod = "projectAccessFallback")
    @Retry(name = "project-service")
    public void validateProjectAccess(Long projectId) {
        try {
            ProjectResponseDTO project = projectFeignClient.getProject(projectId);
            log.debug("Project validated: {} ({})", project.getName(), project.getStatus());
        } catch (FeignException.Forbidden e) {
            throw new AccessDeniedException("Access denied to project: " + projectId);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }
    }

    public void projectAccessFallback(Long projectId, Throwable t) {
        log.error("Project service circuit OPEN for project: {}. Cause: {}", projectId, t.getMessage());
        throw new ServiceUnavailableException("Project service is currently unavailable. Please try again later.");
    }

    @CircuitBreaker(name = "project-service", fallbackMethod = "projectAdminFallback")
    @Retry(name = "project-service")
    public void validateProjectAdmin(Long projectId) {
        try {
            projectFeignClient.validateAdmin(projectId);
        } catch (FeignException.Forbidden e) {
            throw new AccessDeniedException("Only project ADMIN can assign tasks");
        }
    }

    public void projectAdminFallback(Long projectId, Throwable t) {
        log.error("Project service circuit OPEN for admin check: {}. Cause: {}", projectId, t.getMessage());
        throw new ServiceUnavailableException("Project service is currently unavailable. Please try again later.");
    }

    @Override
    public Page<TaskResponseDTO> getTasksByProject(Long projectId, int page, int size,
                                                    String status, String sortBy, String direction) {
        String user = getCurrentUser();
        log.info("ACTION=FETCH_TASKS | USER={} | PROJECT={}", user, projectId);

        validateProjectAccess(projectId);

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Task> taskPage;

        if (status != null && !status.isBlank()) {
            TaskStatus taskStatus;
            try {
                taskStatus = TaskStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid status value: " + status);
            }
            taskPage = taskRepository.findByProjectIdAndStatus(projectId, taskStatus, pageable);
        } else {
            taskPage = taskRepository.findByProjectId(projectId, pageable);
        }

        log.info("ACTION=FETCH_TASKS_SUCCESS | COUNT={}", taskPage.getTotalElements());
        return taskPage.map(this::mapToDTO);
    }

    @Override
    public TaskResponseDTO updateStatus(Long taskId, UpdateTaskStatusDTO request) {
        String user = getCurrentUser();
        log.info("ACTION=UPDATE_TASK_STATUS | USER={} | TASK={} | STATUS={}",
                user, taskId, request.getStatus());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        validateProjectAccess(task.getProjectId());

        TaskStatus taskStatus;
        try {
            taskStatus = TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status value: " + request.getStatus());
        }

        task.setStatus(taskStatus);
        Task updated = taskRepository.save(task);

        log.info("ACTION=UPDATE_TASK_SUCCESS | USER={} | TASK={} | STATUS={}", user, taskId, taskStatus);
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
                .createdAt(task.getCreatedAt() != null
                        ? task.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : null)
                .updatedAt(task.getUpdatedAt() != null
                        ? task.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : null)
                .build();
    }
}