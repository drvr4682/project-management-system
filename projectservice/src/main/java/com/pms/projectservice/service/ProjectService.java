package com.pms.projectservice.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.projectservice.dto.ProjectRequestDTO;
import com.pms.projectservice.dto.ProjectResponseDTO;
import com.pms.projectservice.entity.Project;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.entity.ProjectStatus;
import com.pms.projectservice.exception.ResourceNotFoundException;
import com.pms.projectservice.exception.UnauthorizedException;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.repository.ProjectRepository;
import com.pms.projectservice.security.SecurityUtils;
import com.pms.projectservice.util.AuditLogger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id:" + id));

        projectAccessService.validateAdmin(id, user);

        projectMemberRepository.deleteByProjectId(id);

        projectRepository.delete(project);

        auditLogger.log(user, "DELETE_PROJECT", id, "Deleted project: " + project.getName());
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> getProjects(
            String status, 
            String search,
            int page,
            int size,
            String sortBy,
            String direction) {

        String currentUser =SecurityUtils.getCurrentUser();

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

        List<Long> accessibleProjectIds =
                projectMemberRepository
                        .findByUserId(currentUser)
                        .stream()
                        .map(ProjectMember::getProjectId)
                        .distinct()
                        .toList();

        if (accessibleProjectIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<Project> projectPage =
                projectRepository.findDistinctByIdIn(
                        accessibleProjectIds,
                        pageable
                );

        if (status != null && !status.isBlank()) {

            ProjectStatus projectStatus =
                    ProjectStatus.valueOf(status.toUpperCase());

            List<Project> filtered =
                    projectPage.getContent()
                            .stream()
                            .filter(project ->
                                    project.getStatus() == projectStatus
                            )
                            .toList();

            projectPage =
                    new PageImpl<>(
                            filtered,
                            pageable,
                            filtered.size()
                    );
        }

        if (search != null && !search.isBlank()) {

            String searchLower = search.toLowerCase();

            List<Project> filtered =
                    projectPage.getContent()
                            .stream()
                            .filter(project ->
                                    project.getName()
                                            .toLowerCase()
                                            .contains(searchLower)
                            )
                            .toList();

            projectPage =
                    new PageImpl<>(
                            filtered,
                            pageable,
                            filtered.size()
                    );
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