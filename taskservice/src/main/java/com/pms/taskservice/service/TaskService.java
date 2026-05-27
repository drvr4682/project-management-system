package com.pms.taskservice.service;

import com.pms.common.security.SecurityUtils;
import com.pms.taskservice.dto.AssignTaskRequestDTO;
import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.dto.TaskResponseDTO;
import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskPriority;
import com.pms.taskservice.entity.TaskStatus;
import com.pms.taskservice.exception.AccessDeniedException;
import com.pms.taskservice.exception.ResourceNotFoundException;
import com.pms.taskservice.exception.UnauthorizedException;
import com.pms.taskservice.repository.TaskRepository;
import com.pms.taskservice.util.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectValidationComponent projectValidationComponent;
    private final AuthValidationComponent authValidationComponent;
    private final SecurityUtils securityUtils;
    private final AuditLogger auditLogger;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String requireCurrentUser() {
        String user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return user;
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    /** Only the task creator or a project admin (validated via ProjectService) may modify. */
    private void requireEditAccess(Task task, String currentUser) {
        if (java.util.UUID.fromString(currentUser).equals(task.getCreatedBy())) {
            return; // creator always has edit access
        }
        // Delegate admin check to ProjectService
        try {
            projectValidationComponent.validateProjectAdmin(task.getProjectId());
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(
                    "Only the task creator or a project admin can perform this action"
            );
        }
    }

    private TaskResponseDTO mapToResponse(Task task) {
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .dueDate(task.getDueDate() != null
                        ? task.getDueDate().toInstant(ZoneOffset.UTC).toEpochMilli()
                        : null)
                .projectId(task.getProjectId())
                .createdBy(task.getCreatedBy() != null ? task.getCreatedBy().toString() : null)
                .assignedTo(task.getAssignedTo() != null ? task.getAssignedTo().toString() : null)
                .createdAt(task.getCreatedAt() != null
                        ? task.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                        : null)
                .updatedAt(task.getUpdatedAt() != null
                        ? task.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                        : null)
                .build();
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @Transactional
    public TaskResponseDTO createTask(TaskRequestDTO request) {

        String currentUser = requireCurrentUser();

        log.info(
                "Create Task | User: {} | ProjectId: {} | CorrelationId: {}",
                currentUser, request.getProjectId(), securityUtils.getCorrelationId()
        );

        // Validate project membership via ProjectService
        projectValidationComponent.validateProjectMember(request.getProjectId());

        TaskStatus status;
        try {
            status = request.getStatus() != null
                    ? TaskStatus.valueOf(request.getStatus().toUpperCase())
                    : TaskStatus.TODO;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + request.getStatus());
        }

        TaskPriority priority;
        try {
            priority = request.getPriority() != null
                    ? TaskPriority.valueOf(request.getPriority().toUpperCase())
                    : TaskPriority.MEDIUM;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority value: " + request.getPriority());
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(status)
                .priority(priority)
                .dueDate(request.getDueDate())
                .projectId(request.getProjectId())
                .createdBy(java.util.UUID.fromString(currentUser))
                .build();

        Task saved = taskRepository.save(task);

        auditLogger.log(currentUser, "CREATE_TASK", saved.getId(),
                "project=" + request.getProjectId());

        log.info("Task created | id: {} | project: {}", saved.getId(), request.getProjectId());

        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // GET BY ID
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskById(Long taskId) {

        String currentUser = requireCurrentUser();

        log.info(
                "Get Task | User: {} | TaskId: {} | CorrelationId: {}",
                currentUser, taskId, securityUtils.getCorrelationId()
        );

        Task task = requireTask(taskId);

        // Validate project membership — only project members may see tasks
        projectValidationComponent.validateProjectMember(task.getProjectId());

        return mapToResponse(task);
    }

    // -----------------------------------------------------------------------
    // GET ALL (paginated + filtered + sorted + searched)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<TaskResponseDTO> getTasks(
            Long projectId,
            String status,
            String priority,
            String assignedTo,
            String search,
            int page,
            int size,
            String sortBy,
            String direction) {

        String currentUser = requireCurrentUser();

        log.info(
                "Get Tasks | User: {} | ProjectId: {} | CorrelationId: {}",
                currentUser, projectId, securityUtils.getCorrelationId()
        );

        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Direction must be 'asc' or 'desc'");
        }

        // Validate project membership
        projectValidationComponent.validateProjectMember(projectId);

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        boolean hasStatus   = status     != null && !status.isBlank();
        boolean hasPriority = priority   != null && !priority.isBlank();
        boolean hasSearch   = search     != null && !search.isBlank();
        boolean hasAssignee = assignedTo != null && !assignedTo.isBlank();

        TaskStatus taskStatus = null;
        if (hasStatus) {
            try {
                taskStatus = TaskStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }

        TaskPriority taskPriority = null;
        if (hasPriority) {
            try {
                taskPriority = TaskPriority.valueOf(priority.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid priority: " + priority);
            }
        }

        // Assignee filter takes priority over combined filters for simplicity
        if (hasAssignee) {
            if (hasStatus) {
                return taskRepository
                        .findByProjectIdAndAssignedToAndStatus(projectId, java.util.UUID.fromString(assignedTo), taskStatus, pageable)
                        .map(this::mapToResponse);
            }
            return taskRepository
                    .findByProjectIdAndAssignedTo(projectId, java.util.UUID.fromString(assignedTo), pageable)
                    .map(this::mapToResponse);
        }

        Page<Task> result;

        if (hasStatus && hasPriority && hasSearch) {
            result = taskRepository
                    .findByProjectIdAndStatusAndPriorityAndTitleContainingIgnoreCase(
                            projectId, taskStatus, taskPriority, search, pageable);

        } else if (hasStatus && hasPriority) {
            result = taskRepository
                    .findByProjectIdAndStatusAndPriority(
                            projectId, taskStatus, taskPriority, pageable);

        } else if (hasStatus && hasSearch) {
            result = taskRepository
                    .findByProjectIdAndStatusAndTitleContainingIgnoreCase(
                            projectId, taskStatus, search, pageable);

        } else if (hasPriority && hasSearch) {
            result = taskRepository
                    .findByProjectIdAndPriorityAndTitleContainingIgnoreCase(
                            projectId, taskPriority, search, pageable);

        } else if (hasStatus) {
            result = taskRepository
                    .findByProjectIdAndStatus(projectId, taskStatus, pageable);

        } else if (hasPriority) {
            result = taskRepository
                    .findByProjectIdAndPriority(projectId, taskPriority, pageable);

        } else if (hasSearch) {
            result = taskRepository
                    .findByProjectIdAndTitleContainingIgnoreCase(projectId, search, pageable);

        } else {
            result = taskRepository.findByProjectId(projectId, pageable);
        }

        return result.map(this::mapToResponse);
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    @Transactional
    public TaskResponseDTO updateTask(Long taskId, TaskRequestDTO request) {

        String currentUser = requireCurrentUser();

        log.info(
                "Update Task | User: {} | TaskId: {} | CorrelationId: {}",
                currentUser, taskId, securityUtils.getCorrelationId()
        );

        Task task = requireTask(taskId);

        // Validate project membership first
        projectValidationComponent.validateProjectMember(task.getProjectId());

        // Only creator or project admin may update
        requireEditAccess(task, currentUser);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());

        if (request.getStatus() != null) {
            try {
                task.setStatus(TaskStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status value: " + request.getStatus());
            }
        }

        if (request.getPriority() != null) {
            try {
                task.setPriority(TaskPriority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid priority value: " + request.getPriority());
            }
        }

        Task saved = taskRepository.save(task);

        auditLogger.log(currentUser, "UPDATE_TASK", taskId, null);

        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    @Transactional
    public void deleteTask(Long taskId) {

        String currentUser = requireCurrentUser();

        log.info(
                "Delete Task | User: {} | TaskId: {} | CorrelationId: {}",
                currentUser, taskId, securityUtils.getCorrelationId()
        );

        Task task = requireTask(taskId);

        // Validate project membership first
        projectValidationComponent.validateProjectMember(task.getProjectId());

        // Only creator or project admin may delete
        requireEditAccess(task, currentUser);

        taskRepository.delete(task);

        auditLogger.log(currentUser, "DELETE_TASK", taskId,
                "title=" + task.getTitle());
    }

    // -----------------------------------------------------------------------
    // ASSIGN
    // -----------------------------------------------------------------------

    @Transactional
    public TaskResponseDTO assignTask(Long taskId, AssignTaskRequestDTO request) {

        String currentUser = requireCurrentUser();

        log.info(
                "Assign Task | User: {} | TaskId: {} | Assignee: {} | CorrelationId: {}",
                currentUser, taskId, request.getAssigneeId(), securityUtils.getCorrelationId()
        );

        Task task = requireTask(taskId);

        // Validate project membership
        projectValidationComponent.validateProjectMember(task.getProjectId());

        // Only creator or project admin may assign
        requireEditAccess(task, currentUser);

        // Validate assignee exists in auth service
        String authResponse = authValidationComponent.validateUser(request.getAssigneeId());
        if (!"User exists".equalsIgnoreCase(authResponse)) {
            throw new IllegalArgumentException("Assignee user does not exist");
        }

        task.setAssignedTo(java.util.UUID.fromString(request.getAssigneeId()));

        Task saved = taskRepository.save(task);

        auditLogger.log(currentUser, "ASSIGN_TASK", taskId, request.getAssigneeId());

        log.info("Task {} assigned to {}", taskId, request.getAssigneeId());

        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // REMOVE ASSIGNEE
    // -----------------------------------------------------------------------

    @Transactional
    public TaskResponseDTO removeAssignee(Long taskId) {

        String currentUser = requireCurrentUser();

        log.info(
                "Remove Assignee | User: {} | TaskId: {} | CorrelationId: {}",
                currentUser, taskId, securityUtils.getCorrelationId()
        );

        Task task = requireTask(taskId);

        // Validate project membership
        projectValidationComponent.validateProjectMember(task.getProjectId());

        // Only creator or project admin may remove assignee
        requireEditAccess(task, currentUser);

        task.setAssignedTo(null);

        Task saved = taskRepository.save(task);

        auditLogger.log(currentUser, "REMOVE_ASSIGNEE", taskId, null);

        return mapToResponse(saved);
    }
}