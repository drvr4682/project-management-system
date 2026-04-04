package com.pms.projectservice.controller;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/health")
    public String health() {
        return projectService.healthCheck();
    }

    @GetMapping("/{id}")
    public ProjectResponseDTO getProject(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PutMapping("/{id}")
    public ProjectResponseDTO updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequestDTO request
    ) {
        return projectService.updateProject(id, request);
    }

    @DeleteMapping("/{id}")
    public String deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return "Project deleted successfully";
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDTO> createProject(@Valid @RequestBody ProjectRequestDTO request) {
        return ResponseEntity.ok(projectService.createProject(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponseDTO>> getProjects(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
            
        return ResponseEntity.ok(projectService.getProjects(status, page, size));
    }
}