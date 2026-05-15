package com.pms.projectservice.service;

import com.pms.projectservice.dto.ProjectRequestDTO;
import com.pms.projectservice.dto.ProjectResponseDTO;
import com.pms.projectservice.entity.Project;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.entity.ProjectStatus;
import com.pms.projectservice.exception.ResourceNotFoundException;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.repository.ProjectRepository;
import com.pms.projectservice.util.AuditLogger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private ProjectService projectService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin@test.com",
                        null,
                        Collections.emptyList()
                );

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should create project successfully")
    void shouldCreateProjectSuccessfully() {

        ProjectRequestDTO request = new ProjectRequestDTO();
        request.setName("PMS Backend");
        request.setDescription("Project Desc");
        request.setStatus("ACTIVE");

        Project savedProject = Project.builder()
                .id(1L)
                .name("PMS Backend")
                .description("Project Desc")
                .ownerId("admin@test.com")
                .status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.save(any(Project.class)))
                .thenReturn(savedProject);

        ProjectResponseDTO response =
                projectService.createProject(request);

        assertNotNull(response);
        assertEquals("PMS Backend", response.getName());
        assertEquals("ACTIVE", response.getStatus());

        verify(projectRepository, times(1))
                .save(any(Project.class));

        verify(projectMemberRepository, times(1))
                .save(any());
    }

    @Test
    @DisplayName("Should return project by id")
    void shouldReturnProjectById() {

        Project project = Project.builder()
                .id(1L)
                .name("Backend API")
                .description("Desc")
                .ownerId("admin@test.com")
                .status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.findById(1L))
                .thenReturn(Optional.of(project));

        when(projectAccessService
                .validateMember(1L, "admin@test.com"))
                .thenReturn(
                        ProjectMember.builder()
                                .projectId(1L)
                                .userId("admin@test.com")
                                .role(ProjectRole.ADMIN)
                                .build()
                );

        ProjectResponseDTO response =
                projectService.getProjectById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Backend API", response.getName());
    }

    @Test
    @DisplayName("Should throw exception when project not found")
    void shouldThrowExceptionWhenProjectNotFound() {

        when(projectRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> projectService.getProjectById(999L)
        );
    }

    @Test
    @DisplayName("Should update project successfully")
    void shouldUpdateProjectSuccessfully() {

        Project existing = Project.builder()
                .id(1L)
                .name("Old Name")
                .description("Old Desc")
                .ownerId("admin@test.com")
                .status(ProjectStatus.ACTIVE)
                .build();

        ProjectRequestDTO request = new ProjectRequestDTO();
        request.setName("New Name");
        request.setDescription("New Desc");
        request.setStatus("COMPLETED");

        when(projectRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        doNothing().when(projectAccessService)
                .validateAdmin(1L, "admin@test.com");

        when(projectRepository.save(any(Project.class)))
                .thenReturn(existing);

        ProjectResponseDTO response =
                projectService.updateProject(1L, request);

        assertEquals("New Name", response.getName());
        assertEquals("COMPLETED", response.getStatus());
    }

    @Test
    @DisplayName("Should delete project successfully")
    void shouldDeleteProjectSuccessfully() {

        Project project = Project.builder()
                .id(1L)
                .name("Delete Project")
                .description("Desc")
                .ownerId("admin@test.com")
                .status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.findById(1L))
                .thenReturn(Optional.of(project));

        doNothing().when(projectAccessService)
                .validateAdmin(1L, "admin@test.com");

        doNothing().when(projectRepository)
                .delete(project);

        projectService.deleteProject(1L);

        verify(projectMemberRepository, times(1))
                .deleteByProjectId(1L);

        verify(projectRepository, times(1))
                .delete(project);
    }

    @Test
    @DisplayName("Should return paginated accessible projects")
    void shouldReturnPaginatedAccessibleProjects() {

        Project project = Project.builder()
                .id(1L)
                .name("Backend")
                .description("Desc")
                .ownerId("admin@test.com")
                .status(ProjectStatus.ACTIVE)
                .build();

        ProjectMember member = ProjectMember.builder()
                .projectId(1L)
                .userId("admin@test.com")
                .role(ProjectRole.ADMIN)
                .build();

        Page<Project> page =
                new PageImpl<>(List.of(project));

        when(projectMemberRepository.findByUserId("admin@test.com"))
                .thenReturn(List.of(member));

        when(projectRepository.findDistinctByIdIn(
                anyList(),
                any(Pageable.class)
        )).thenReturn(page);

        Page<ProjectResponseDTO> result =
                projectService.getProjects(
                        null,
                        null,
                        0,
                        10,
                        "name",
                        "asc"
                );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Should throw exception for invalid sort direction")
    void shouldThrowExceptionForInvalidSortDirection() {

        assertThrows(
                IllegalArgumentException.class,
                () -> projectService.getProjects(
                        null,
                        null,
                        0,
                        10,
                        "name",
                        "random"
                )
        );
    }
}