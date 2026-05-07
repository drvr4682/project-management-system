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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogger auditLogger;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String healthCheck() {
        return "Project Service is running";
    }

    private String getCurrentUser() {
        String user = SecurityUtils.getCurrentUser();
        if (user == null) throw new UnauthorizedException("Unauthorized");
        return user;
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    private Long toEpochMilli(LocalDateTime dt) {
        return dt != null ? dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    // ── Write operations — transactional ─────────────────────────────────────

    /**
     * Creates a project AND adds the creator as ADMIN member atomically.
     * If the member save fails, the project insert is rolled back.
     * Without @Transactional this would leave an orphaned project in the DB.
     */
    @Transactional
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

        // This second save is inside the same transaction.
        // If it throws, both saves roll back atomically.
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

    /**
     * Updating project fields. @Transactional ensures the dirty-check
     * flush and the audit log happen atomically.
     */
    @Transactional
    public ProjectResponseDTO updateProject(Long id, ProjectRequestDTO request) {
        String user = getCurrentUser();

        Project project = findProjectById(id);

        projectAccessService.validateAdmin(id, user);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        if (request.getStatus() != null) {
            project.setStatus(parseStatus(request.getStatus(), project.getStatus()));
        }

        auditLogger.log(user, "UPDATE_PROJECT", id, null);
        return mapToResponse(projectRepository.save(project));
    }

    /**
     * Delete must be transactional — if something fails after delete
     * (e.g. audit log throws) the delete should roll back.
     */
    @Transactional
    public void deleteProject(Long id) {
        String user = getCurrentUser();

        Project project = findProjectById(id);

        projectAccessService.validateAdmin(id, user);
        projectRepository.delete(project);
        auditLogger.log(user, "DELETE_PROJECT", id, null);
        log.info("ACTION=DELETE_PROJECT | USER={} | PROJECT={}", user, id);
    }

    // ── Read operations — readOnly = true ─────────────────────────────────────

    /**
     * readOnly = true tells Hibernate to skip dirty checking on loaded entities,
     * which improves performance on reads. It also sets the connection to
     * read-only at the JDBC level if the driver supports it.
     */
    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(Long id) {
        String user = getCurrentUser();

        Project project = findProjectById(id);

        projectAccessService.validateMember(id, user);
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponseDTO> getProjects(String status, String search,
            int page, int size, String sortBy, String direction) {

        Sort sort = "desc".equals(direction.toLowerCase(Locale.ROOT))
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        ProjectStatus projectStatus = null;
        if (status != null) {
            projectStatus = parseStatus(status, null);
        }

        Page<Project> projectPage;
        String normalizedSearch = search != null ? search.toLowerCase(Locale.ROOT) : null;

        if (projectStatus != null && normalizedSearch != null) {
            projectPage = projectRepository.findByStatusAndNameContaining(projectStatus, normalizedSearch, pageable);
        } else if (projectStatus != null) {
            projectPage = projectRepository.findByStatus(projectStatus, pageable);
        } else if (normalizedSearch != null) {
            projectPage = projectRepository.findByNameContaining(normalizedSearch, pageable);
        } else {
            projectPage = projectRepository.findAll(pageable);
        }

        return projectPage.map(this::mapToResponse);
    }

    /**
     * This is called by TaskService via Feign — read-only is appropriate.
     * The admin check is a read, not a write.
     */
    @Transactional(readOnly = true)
    public void validateAdmin(Long projectId) {
        String user = getCurrentUser();
        findProjectById(projectId);
        projectAccessService.validateAdmin(projectId, user);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ProjectStatus parseStatus(String value, ProjectStatus fallback) {
        if (value == null) return fallback;
        try {
            return ProjectStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + value, e);
        }
    }

    private ProjectResponseDTO mapToResponse(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .owner(project.getOwnerId())
                .status(project.getStatus().name())
                .createAt(toEpochMilli(project.getCreatedAt()))
                .updateAt(toEpochMilli(project.getUpdatedAt()))
                .build();
    }
}