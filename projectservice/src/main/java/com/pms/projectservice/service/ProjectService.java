package com.pms.projectservice.service;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.repository.ProjectRepository;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.exception.*;
import com.pms.projectservice.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static com.pms.projectservice.security.JwtFilter.currentUser;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final Environment environment;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberService projectMemberService;

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

        String user = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        getProjectMember(id, user); // just check membership

        return mapToResponse(project);
    }

    private ProjectResponseDTO mapToResponse(Project project) {
        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .owner(project.getOwnerId())
                .status(project.getStatus().name())
                .createAt(project.getCreatedAt() != null ? project.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .updateAt(project.getUpdatedAt() != null ? project.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .build();
    }

    public ProjectResponseDTO updateProject(Long id, ProjectRequestDTO request) {

        String user = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        ProjectMember member = getProjectMember(id, user);

        if (!member.getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Only ADMIN can update project");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return mapToResponse(projectRepository.save(project));
    }

    public void deleteProject(Long id) {

        String user = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectMember member = getProjectMember(id, user);

        if (!member.getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Only ADMIN can delete project");
        }

        projectRepository.delete(project);
    }

    public Page<ProjectResponseDTO> getProjects(
            String status, 
            String search,
            int page,
            int size,
            String sortBy,
            String direction) {
        
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

        if (status != null && search != null) {
            projectPage = projectRepository
                    .findByStatusAndNameContainingIgnoreCase(projectStatus, search, pageable);

        } else if (status != null) {
            projectPage = projectRepository
                    .findByStatus(projectStatus, pageable);

        } else if (search != null) {
            projectPage = projectRepository
                    .findByNameContainingIgnoreCase(search, pageable);
                    
        } else {
            projectPage = projectRepository.findAll(pageable);
        }

        return projectPage.map(this::mapToResponse);
    }

    private ProjectMember getProjectMember(Long projectId, String userId) {
        return projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("User not part of project"));
    }
}