package com.pms.projectservice.service;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.repository.ProjectRepository;
import com.pms.projectservice.security.SecurityUtils;
import com.pms.projectservice.util.AuditLogger;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.exception.*;
import com.pms.projectservice.entity.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogger auditLogger;

    public String healthCheck() {
        return "Project Service is running";
    }

    private String getCurrentUser() {
        String user = SecurityUtils.getCurrentUser();

        if (user == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        return user;
    }

    @Transactional
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

        // ADD OWNER AS PROJECT ADMIN
        ProjectMember member = ProjectMember.builder()
                .projectId(saved.getId())
                .userId(currentUser)
                .role(ProjectRole.ADMIN)
                .build();

        projectMemberRepository.save(member);

        auditLogger.log(currentUser, "CREATE_PROJECT", saved.getId(), null);

        log.info("Owner added as ADMIN in project_members");

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(Long id) {

        String user = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        projectAccessService.validateMember(id, user);
        
        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponseDTO updateProject(Long id, ProjectRequestDTO request) {

        String user = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        projectAccessService.validateAdmin(id, user);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        if (request.getStatus() != null) {
            project.setStatus(
                    ProjectStatus.valueOf(
                            request.getStatus().toUpperCase()
                    )
            );
        }

        auditLogger.log(user, "UPDATE_PROJECT", id, null);

        return mapToResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {

        String user = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        projectAccessService.validateAdmin(id, user);

        projectRepository.delete(project);

        auditLogger.log(user, "DELETE_PROJECT", id, null);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> getProjects(
            String status, 
            String search,
            int page,
            int size,
            String sortBy,
            String direction) {

        List<String> allowedSortFields =
                List.of("name", "createdAt", "updatedAt", "status");

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field");
        }
        
        if (!direction.equalsIgnoreCase("asc")
                && !direction.equalsIgnoreCase("desc")) {

            throw new IllegalArgumentException(
                    "Direction must be asc or desc"
            );
        }

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();


        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Project> projectPage;

        ProjectStatus projectStatus = null;

        if(status != null) {
            try {
                projectStatus = ProjectStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid status value");
            }
        }

        boolean hasStatus = status != null && !status.isBlank();
        boolean hasSearch = search != null && !search.isBlank();

        if (hasStatus && hasSearch) {

            projectPage = projectRepository
                    .findByStatusAndNameContainingIgnoreCase(
                            projectStatus,
                            search,
                            pageable
                    );

        } else if (hasStatus) {
            projectPage = projectRepository
                    .findByStatus(projectStatus, pageable);
        } else if (hasSearch) {
            projectPage = projectRepository
                    .findByNameContainingIgnoreCase(search, pageable);
        } else {
            projectPage = projectRepository.findAll(pageable);
        }

        return projectPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public void validateAdmin(Long projectId) {

        String user = SecurityUtils.getCurrentUser();

        if (user == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        // check project exists
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // RBAC check
        projectAccessService.validateAdmin(projectId, user);
    }

    
    private ProjectResponseDTO mapToResponse(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .owner(project.getOwnerId())
                .status(project.getStatus() != null ? project.getStatus().name() : null)
                .createdAt(project.getCreatedAt() != null ? project.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .updatedAt(project.getUpdatedAt() != null ? project.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .build();
    }
}