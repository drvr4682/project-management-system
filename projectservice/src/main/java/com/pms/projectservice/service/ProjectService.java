package com.pms.projectservice.service;

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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

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
        if (user == null) throw new UnauthorizedException("Unauthorized");
        return user;
    }

    public ProjectResponseDTO createProject(ProjectRequestDTO request) {
        String currentUser = getCurrentUser();
        log.info("ACTION=CREATE_PROJECT | USER={}", currentUser);

        ProjectStatus status = parseStatus(request.getStatus(), ProjectStatus.ACTIVE);

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(currentUser)
                .status(status)
                .build();

        Project saved = projectRepository.save(project);

        // Auto-add creator as project ADMIN
        ProjectMember member = ProjectMember.builder()
                .projectId(saved.getId())
                .userId(currentUser)
                .role(ProjectRole.ADMIN)
                .build();
        projectMemberRepository.save(member);

        auditLogger.log(currentUser, "CREATE_PROJECT", saved.getId(), null);
        log.info("ACTION=CREATE_PROJECT_SUCCESS | USER={} | PROJECT={}", currentUser, saved.getId());

        return mapToResponse(saved);
    }

    public ProjectResponseDTO getProjectById(Long id) {
        String user = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        projectAccessService.validateMember(id, user);
        return mapToResponse(project);
    }

    public ProjectResponseDTO updateProject(Long id, ProjectRequestDTO request) {
        String user = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        projectAccessService.validateAdmin(id, user);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        // Also update status if provided
        if (request.getStatus() != null) {
            project.setStatus(parseStatus(request.getStatus(), project.getStatus()));
        }

        auditLogger.log(user, "UPDATE_PROJECT", id, null);
        return mapToResponse(projectRepository.save(project));
    }

    public void deleteProject(Long id) {
        String user = getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        projectAccessService.validateAdmin(id, user);
        projectRepository.delete(project);
        auditLogger.log(user, "DELETE_PROJECT", id, null);
        log.info("ACTION=DELETE_PROJECT | USER={} | PROJECT={}", user, id);
    }

    public Page<ProjectResponseDTO> getProjects(String status, String search,
            int page, int size, String sortBy, String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        ProjectStatus projectStatus = null;
        if (status != null) {
            projectStatus = parseStatus(status, null);
        }

        Page<Project> projectPage;

        if (projectStatus != null && search != null) {
            projectPage = projectRepository.findByStatusAndNameContainingIgnoreCase(projectStatus, search, pageable);
        } else if (projectStatus != null) {
            projectPage = projectRepository.findByStatus(projectStatus, pageable);
        } else if (search != null) {
            projectPage = projectRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            projectPage = projectRepository.findAll(pageable);
        }

        return projectPage.map(this::mapToResponse);
    }

    public void validateAdmin(Long projectId) {
        String user = getCurrentUser();
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        projectAccessService.validateAdmin(projectId, user);
    }

    private ProjectStatus parseStatus(String value, ProjectStatus fallback) {
        if (value == null) return fallback;
        try {
            return ProjectStatus.valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status value: " + value);
        }
    }

    private ProjectResponseDTO mapToResponse(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .owner(project.getOwnerId())
                .status(project.getStatus().name())
                .createAt(project.getCreatedAt() != null
                        ? project.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : null)
                .updateAt(project.getUpdatedAt() != null
                        ? project.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : null)
                .build();
    }
}