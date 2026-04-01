package com.pms.projectservice.service;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.entity.Project;
import com.pms.projectservice.repository.ProjectRepository;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.exception.*;
import com.pms.projectservice.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import static com.pms.projectservice.security.JwtFilter.currentUser;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final Environment environment;
    private final ProjectMemberRepository projectMemberRepository;

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

        String currentUser = getCurrentUser();

        log.info("Creating project for user: {}", currentUser);

        // Status handling
        ProjectStatus status;
        try {
            status = request.getStatus() != null
                    ? ProjectStatus.valueOf(request.getStatus().toUpperCase())
                    : ProjectStatus.ACTIVE;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status value");
        }

        // Create project
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(currentUser)
                .status(status)
                .build();

        Project saved = projectRepository.save(project);

        log.info("Project created with ID: {}", saved.getId());

        // ✅ ADD THIS BLOCK (CRITICAL)
        ProjectMember member = ProjectMember.builder()
                .projectId(saved.getId())
                .userId(currentUser)
                .role(ProjectRole.ADMIN)
                .build();

        projectMemberRepository.save(member);

        log.info("Owner added as ADMIN in project_members");

        return mapToResponse(saved);
    }

    public ProjectResponseDTO getProjectById(Long id) {

        String currentUser = getCurrentUser();

        log.info("Fetching project ID: {} by user: {}", id, currentUser);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Project not found: {}", id);
                    return new ResourceNotFoundException("Project not found");
                });

        if (!project.getOwnerId().equals(currentUser)) {
            log.warn("Access denied for user: {} on project ID: {}", currentUser, id);
            throw new AccessDeniedException("Access denied");
        }

        return mapToResponse(project);
    }

    private ProjectResponseDTO mapToResponse(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .owner(project.getOwnerId())
                .build();
    }

    public ProjectResponseDTO updateProject(Long id, ProjectRequestDTO request) {

        String currentUser = getCurrentUser();

        log.info("Updating project ID: {} by user: {}", id, currentUser);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Project not found: {}", id);
                    return new ResourceNotFoundException("Project not found");
                });

        if (!project.getOwnerId().equals(currentUser)) {
            log.warn("Unauthorized update attempt by user: {} on project ID: {}", currentUser, id);
            throw new AccessDeniedException("Access denied");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project updated = projectRepository.save(project);

        log.info("Project updated successfully: {}", id);

        return mapToResponse(updated);
    }

    public void deleteProject(Long id) {

        String currentUser = getCurrentUser();

        log.info("Deleting project ID: {} by user: {}", id, currentUser);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Project not found: {}", id);
                    return new ResourceNotFoundException("Project not found");
                });

        if (!project.getOwnerId().equals(currentUser)) {
            log.warn("Unauthorized delete attempt by user: {} on project ID: {}", currentUser, id);
            throw new AccessDeniedException("Access denied");
        }

        projectRepository.delete(project);

        log.info("Project deleted successfully: {}", id);
    }
}