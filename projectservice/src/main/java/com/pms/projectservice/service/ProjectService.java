package com.pms.projectservice.service;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.entity.Project;
import com.pms.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public String healthCheck() {
        return "Project Service is running";
    }

    public ProjectResponseDTO createProject(ProjectRequestDTO request) {

        String currentUser = "TEMP_USER"; // placeholder (JWT later)

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(currentUser)
                .build();

        Project saved = projectRepository.save(project);

        return mapToResponse(saved);
    }

    public ProjectResponseDTO getProjectById(Long id) {

        String currentUser = "TEMP_USER";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwner().equals(currentUser)) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(project);
    }

    private ProjectResponseDTO mapToResponse(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .owner(project.getOwner())
                .build();
    }
}