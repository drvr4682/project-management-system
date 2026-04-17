package com.pms.taskservice.controller;

import com.pms.taskservice.dto.*;
import com.pms.taskservice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // ✅ CREATE TASK
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
            @Valid @RequestBody TaskRequestDTO request) {

        return ResponseEntity.ok(taskService.createTask(request));
    }

    // ✅ GET TASKS BY PROJECT
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<TaskResponseDTO>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
        ) {

        return ResponseEntity.ok(taskService.getTasksByProject(projectId, page, size, status, sortBy, direction));
    }

    // ✅ UPDATE STATUS
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/{taskId}/status")
    public ResponseEntity<TaskResponseDTO> updateStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusDTO request) {

        return ResponseEntity.ok(taskService.updateStatus(taskId, request));
    }
}