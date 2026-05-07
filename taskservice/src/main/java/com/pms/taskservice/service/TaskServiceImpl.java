package com.pms.taskservice.service;

import com.pms.common.client.AuthFeignClient;
import com.pms.common.client.ProjectFeignClient;
import com.pms.common.dto.ProjectSummaryDTO;
import com.pms.common.dto.UserExistsResponse;
import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.dto.TaskResponseDTO;
import com.pms.taskservice.dto.UpdateTaskStatusDTO;
import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskStatus;
import com.pms.taskservice.exception.AccessDeniedException;
import com.pms.taskservice.exception.ResourceNotFoundException;
import com.pms.taskservice.exception.ServiceUnavailableException;
import com.pms.taskservice.repository.TaskRepository;

import feign.FeignException;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

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

    /**
     * createTask orchestrates: HTTP validations first, then DB write.
     * The DB write is isolated in a short @Transactional method.
     * HTTP calls (validateUser, validateProjectAccess, validateProjectAdmin)
     * are deliberately outside the DB transaction.
     */
    @Override
    public TaskResponseDTO createTask(TaskRequestDTO request) {
        String user = getCurrentUser();
        log.info("ACTION=CREATE_TASK | USER={} | PROJECT={} | ASSIGNED_TO={}",
                user, request.getProjectId(), request.getAssignedTo());

        // All three are HTTP calls — outside DB transaction
        validateUser(request.getAssignedTo());
        validateProjectAccess(request.getProjectId());
        validateProjectAdmin(request.getProjectId());

        // DB write — short, atomic transaction
        return persistTask(request, user);
    }

    @Transactional
    protected TaskResponseDTO persistTask(TaskRequestDTO request, String user) {
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

    /**
     * getTasksByProject: HTTP validation first, then read-only DB query.
     * The DB read uses readOnly = true for performance.
     */
    @Override
    public Page<TaskResponseDTO> getTasksByProject(Long projectId, int page, int size,
                                                    String status, String sortBy, String direction) {
        String user = getCurrentUser();
        log.info("ACTION=FETCH_TASKS | USER={} | PROJECT={}", user, projectId);

        // HTTP call — outside transaction
        validateProjectAccess(projectId);

        // DB read — readOnly transaction
        return fetchTasksByProject(projectId, page, size, status, sortBy, direction);
    }

    @Transactional(readOnly = true)
    protected Page<TaskResponseDTO> fetchTasksByProject(Long projectId, int page, int size,
                                                         String status, String sortBy, String direction) {
        Sort sort = "desc".equals(direction.toLowerCase(Locale.ROOT))
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Task> taskPage;

        if (status != null && !status.isBlank()) {
            TaskStatus taskStatus;
            try {
                taskStatus = TaskStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status value: " + status, e);
            }
            taskPage = taskRepository.findByProjectIdAndStatus(projectId, taskStatus, pageable);
        } else {
            taskPage = taskRepository.findByProjectId(projectId, pageable);
        }

        log.info("ACTION=FETCH_TASKS_SUCCESS | COUNT={}", taskPage.getTotalElements());
        return taskPage.map(this::mapToDTO);
    }

    /**
     * updateStatus: read task (to get projectId), HTTP validation, then DB write.
     * We do the task lookup first (cheap DB read), then HTTP, then write.
     */
    @Override
    public TaskResponseDTO updateStatus(Long taskId, UpdateTaskStatusDTO request) {
        String user = getCurrentUser();
        log.info("ACTION=UPDATE_TASK_STATUS | USER={} | TASK={} | STATUS={}",
                user, taskId, request.getStatus());

        // Read task to get projectId — short read-only DB call
        Task task = findTaskById(taskId);

        // HTTP call — outside DB transaction
        validateProjectAccess(task.getProjectId());

        TaskStatus taskStatus;
        try {
            taskStatus = TaskStatus.valueOf(request.getStatus().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + request.getStatus(), e);
        }

        // DB write — short transaction
        return persistStatusUpdate(task, taskStatus, user);
    }

    @Transactional(readOnly = true)
    protected Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }

    @Transactional
    protected TaskResponseDTO persistStatusUpdate(Task task, TaskStatus newStatus, String user) {
        task.setStatus(newStatus);
        Task updated = taskRepository.save(task);
        log.info("ACTION=UPDATE_TASK_SUCCESS | USER={} | TASK={} | STATUS={}", user, updated.getId(), newStatus);
        return mapToDTO(updated);
    }

    // ── Resilience-wrapped HTTP validation methods ────────────────────────────
    // These are intentionally NOT @Transactional — they make HTTP calls.

    @CircuitBreaker(name = "auth-service", fallbackMethod = "userValidationFallback")
    @Retry(name = "auth-service")
    public void validateUser(String email) {
        try {
            UserExistsResponse response = authFeignClient.checkUser(email);
            if (!response.isExists()) {
                throw new IllegalArgumentException("User does not exist: " + email);
            }
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("User does not exist: " + email, e);
        }
    }

    public void userValidationFallback(String email, Throwable t) {
        log.error("Auth circuit OPEN — email: {}, cause: {}", email, t.getMessage());
        throw new ServiceUnavailableException("Auth service is currently unavailable");
    }

    @CircuitBreaker(name = "project-service", fallbackMethod = "projectAccessFallback")
    @Retry(name = "project-service")
    public void validateProjectAccess(Long projectId) {
        try {
            ProjectSummaryDTO project = projectFeignClient.getProject(projectId);
            if (project == null) {
                throw new ServiceUnavailableException("Project service unavailable");
            }
            log.debug("Project validated: {} status={}", project.getName(), project.getStatus());
        } catch (FeignException.Forbidden e) {
            throw new AccessDeniedException("Access denied to project: " + projectId, e);
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Project not found: " + projectId, e);
        }
    }

    public void projectAccessFallback(Long projectId, Throwable t) {
        log.error("Project circuit OPEN — projectId: {}, cause: {}", projectId, t.getMessage());
        throw new ServiceUnavailableException("Project service is currently unavailable");
    }

    @CircuitBreaker(name = "project-service", fallbackMethod = "projectAdminFallback")
    @Retry(name = "project-service")
    public void validateProjectAdmin(Long projectId) {
        try {
            projectFeignClient.validateAdmin(projectId);
        } catch (FeignException.Forbidden e) {
            throw new AccessDeniedException("Only project ADMIN can assign tasks", e);
        }
    }

    public void projectAdminFallback(Long projectId, Throwable t) {
        log.error("Project circuit OPEN (admin check) — projectId: {}, cause: {}", projectId, t.getMessage());
        throw new ServiceUnavailableException("Project service is currently unavailable");
    }

    private Long toEpochMilli(LocalDateTime dt) {
        return dt != null ? dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    private TaskResponseDTO mapToDTO(Task task) {
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .projectId(task.getProjectId())
                .assignedTo(task.getAssignedTo())
                .status(task.getStatus().name())
                .createdAt(toEpochMilli(task.getCreatedAt()))
                .updatedAt(toEpochMilli(task.getUpdatedAt()))
                .build();
    }
}