package com.pms.projectservice.service;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.entity.Project;
import com.pms.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.pms.projectservice.security.JwtFilter.currentUser;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public String healthCheck() {
        return "Project Service is running";
    }

    private String getCurrentUser() {
        String user = currentUser.get();
        return (user != null) ? user : "TEST_USER";
    }

    public ProjectResponseDTO createProject(ProjectRequestDTO request) {

        String currentUserName = getCurrentUser();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(currentUserName)
                .build();

        return mapToResponse(projectRepository.save(project));
    }

    public ProjectResponseDTO getProjectById(Long id) {

        String currentUser = getCurrentUser();

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