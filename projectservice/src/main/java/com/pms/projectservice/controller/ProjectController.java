package com.pms.projectservice.controller;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    public ProjectResponseDTO createProject(@Valid @RequestBody ProjectRequestDTO request) {
        return projectService.createProject(request);
    }

    @GetMapping("/{id}")
    public ProjectResponseDTO getProject(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }
}