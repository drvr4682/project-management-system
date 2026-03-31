package com.pms.projectservice.service;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.entity.Project;
import com.pms.projectservice.repository.ProjectRepository;
import com.pms.projectservice.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import static com.pms.projectservice.security.JwtFilter.currentUser;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final Environment environment;

    public String healthCheck() {
        return "Project Service is running";
    }

    private String getCurrentUser() {
        String user = currentUser.get();

        // allow fallback ONLY in test profile
        boolean isTest = false;
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equals("test")) {
                isTest = true;
                break;
            }
        }

        if (user == null) {
            if (isTest) {
                return "TEST_USER";
            }
            throw new UnauthorizedException("Unauthorized");
        }

        return user;
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
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getOwner().equals(currentUser)) {
            throw new UnauthorizedException("Unauthorized");
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

    public ProjectResponseDTO updateProject(Long id, ProjectRequestDTO request) {

        String currentUser = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getOwner().equals(currentUser)) {
            throw new AccessDeniedException("Access denied");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project updated = projectRepository.save(project);

        return mapToResponse(updated);
    }

    public void deleteProject(Long id) {

        String currentUser = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getOwner().equals(currentUser)) {
            throw new AccessDeniedException("Access denied");
        }

        projectRepository.delete(project);
    }
}