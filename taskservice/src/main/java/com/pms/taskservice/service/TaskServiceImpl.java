package com.pms.taskservice.service;

import com.pms.taskservice.client.AuthFeignClient;
import com.pms.taskservice.client.ProjectFeignClient;
import com.pms.taskservice.dto.*;
import com.pms.taskservice.entity.*;
import com.pms.taskservice.repository.TaskRepository;
import com.pms.taskservice.exception.AccessDeniedException;
import com.pms.taskservice.exception.ServiceUnavailableException;
import com.pms.taskservice.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import feign.FeignException;
import feign.RetryableException;

import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final AuthFeignClient authFeignClient;
    private final ProjectFeignClient projectFeignClient;

    @Override
    public TaskResponseDTO createTask(TaskRequestDTO request) {

        String user = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        log.info("ACTION=CREATE_TASK_REQUEST | USER={} | PROJECT={} | ASSIGNED_TO={}",
                user, request.getProjectId(), request.getAssignedTo());

        // user validation
        try {
            String response = authFeignClient.checkUser(request.getAssignedTo());

            if (!"User exists".equalsIgnoreCase(response)) {
                log.warn("ACTION=CREATE_TASK_FAILED | REASON=INVALID_USER | USER={}", request.getAssignedTo());
                throw new IllegalArgumentException("User does not exist");
            }

        } catch (FeignException.NotFound e) {
            log.warn("ACTION=CREATE_TASK_FAILED | REASON=USER_NOT_FOUND | USER={}", request.getAssignedTo());
            throw new IllegalArgumentException("User does not exist");

        } catch (RetryableException e) {
            log.error("ACTION=CREATE_TASK_FAILED | REASON=AUTH_SERVICE_DOWN");
            throw new ServiceUnavailableException("Auth service unavailable");
        }

        // project validation
        try {
            projectFeignClient.getProject(request.getProjectId());

        } catch (FeignException.Forbidden e) {
            log.warn("ACTION=CREATE_TASK_FAILED | REASON=ACCESS_DENIED | USER={} | PROJECT={}",
                    user, request.getProjectId());
            throw new AccessDeniedException("Access denied to project");

        } catch (FeignException.NotFound e) {
            log.warn("ACTION=CREATE_TASK_FAILED | REASON=PROJECT_NOT_FOUND | PROJECT={}",
                    request.getProjectId());
            throw new IllegalArgumentException("Project not found");
        } catch (RetryableException e) {
            throw new ServiceUnavailableException("Project service unavailable");
        } 

        try {
            projectFeignClient.validateAdmin(request.getProjectId());
        } catch (FeignException.Forbidden e) {
            throw new AccessDeniedException("Only project ADMIN can assign tasks");
        } catch (RetryableException e) {
            throw new ServiceUnavailableException("Project service unavailable");
        }

        Task saved = taskRepository.save(Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .assignedTo(request.getAssignedTo())
                .status(TaskStatus.TODO)
                .build());

        log.info("ACTION=CREATE_TASK_SUCCESS | USER={} | PROJECT={} | TASK_ID={}",
                user, saved.getProjectId(), saved.getId());

        return mapToDTO(saved);
    }

    @Override
    public Page<TaskResponseDTO> getTasksByProject(
                    Long projectId,
                    int page,
                    int size,
                    String status,
                    String sortBy,
                    String direction) {

        String user = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        log.info("ACTION=FETCH_TASKS | USER={} | PROJECT={}", user, projectId);

        try {
            projectFeignClient.getProject(projectId);
        } catch (FeignException.Forbidden e) {
            log.warn("ACTION=FETCH_TASKS_FAILED | REASON=ACCESS_DENIED | USER={} | PROJECT={}",
                    user, projectId);
            throw new AccessDeniedException("Access denied to project");
        } catch (RetryableException e) {
            throw new ServiceUnavailableException("Project service unavailable");
        }

        Sort sort = 
            direction.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() :
                    Sort.by(sortBy).ascending();

        Pageable pageable = 
                PageRequest.of(page, size, sort);
        
        Page<Task> taskPage;

        if (status != null && !status.isBlank()) {
            TaskStatus taskStatus;
            try {
                taskStatus = TaskStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid status value");
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

        String user = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        log.info("ACTION=UPDATE_TASK_STATUS | USER={} | TASK={} | STATUS={}",
                user, taskId, request.getStatus());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        try {
            projectFeignClient.getProject(task.getProjectId());
        } catch (FeignException.Forbidden e) {
            log.warn("ACTION=UPDATE_TASK_FAILED | REASON=ACCESS_DENIED | USER={} | PROJECT={}",
                    user, task.getProjectId());
            throw new AccessDeniedException("Access denied to project");
        } catch (RetryableException e) {
            throw new ServiceUnavailableException("Project service unavailable");
        }

        TaskStatus status;
        try {
            status = TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            log.warn("ACTION=UPDATE_TASK_FAILED | REASON=INVALID_STATUS | INPUT={}", request.getStatus());
            throw new IllegalArgumentException("Invalid status value");
        }

        task.setStatus(status);
        Task updated = taskRepository.save(task);

        log.info("ACTION=UPDATE_TASK_SUCCESS | USER={} | TASK={} | STATUS={}",
                user, taskId, status);

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