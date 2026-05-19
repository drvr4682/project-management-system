package com.pms.taskservice.controller;

import com.pms.taskservice.dto.AssignTaskRequestDTO;
import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.dto.TaskResponseDTO;
import com.pms.taskservice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponseDTO> createTask(
            @Valid @RequestBody TaskRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(taskService.createTask(request));
    }

    // -----------------------------------------------------------------------
    // GET BY ID
    // -----------------------------------------------------------------------

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponseDTO> getTask(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<TaskResponseDTO>> getTasks(
            @RequestParam Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "10")         int size,
            @RequestParam(defaultValue = "createdAt")  String sortBy,
            @RequestParam(defaultValue = "desc")       String direction) {

        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        return ResponseEntity.ok(
                taskService.getTasks(
                        projectId, status, priority, assignedTo,
                        search, page, size, sortBy, direction)
        );
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    @PutMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequestDTO request) {

        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String> deleteTask(
            @PathVariable Long taskId) {

        taskService.deleteTask(taskId);
        return ResponseEntity.ok("Task deleted successfully");
    }

    // -----------------------------------------------------------------------
    // ASSIGN
    // -----------------------------------------------------------------------

    @PutMapping("/{taskId}/assign")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponseDTO> assignTask(
            @PathVariable Long taskId,
            @Valid @RequestBody AssignTaskRequestDTO request) {

        return ResponseEntity.ok(taskService.assignTask(taskId, request));
    }

    // -----------------------------------------------------------------------
    // REMOVE ASSIGNEE
    // -----------------------------------------------------------------------

    @DeleteMapping("/{taskId}/assign")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponseDTO> removeAssignee(
            @PathVariable Long taskId) {

        return ResponseEntity.ok(taskService.removeAssignee(taskId));
    }
}
