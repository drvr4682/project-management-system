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
import com.pms.projectservice.util.AuditLogger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectServiceTest {

    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectAccessService projectAccessService;
    private AuditLogger auditLogger;
    private ProjectService projectService;

    @BeforeEach
    void setup() {
        projectRepository = Mockito.mock(ProjectRepository.class);
        projectMemberRepository = Mockito.mock(ProjectMemberRepository.class);
        projectAccessService = Mockito.mock(ProjectAccessService.class);
        auditLogger = Mockito.mock(AuditLogger.class);

        projectService = new ProjectService(
                projectRepository,
                projectMemberRepository,
                projectAccessService,
                auditLogger
        );

        // Set up authenticated user in security context
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("owner@test.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // ── createProject tests ───────────────────────────────────────────────────

    @Test
    void createProject_shouldSaveProjectAndMemberAtomically() {
        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Test Project")
                .description("Desc")
                .build();

        Project savedProject = Project.builder()
                .id(1L)
                .name("Test Project")
                .description("Desc")
                .ownerId("owner@test.com")
                .status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(
                ProjectMember.builder()
                        .id(1L)
                        .projectId(1L)
                        .userId("owner@test.com")
                        .role(ProjectRole.ADMIN)
                        .build()
        );

        ProjectResponseDTO response = projectService.createProject(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Project", response.getName());
        assertEquals("ACTIVE", response.getStatus());

        // Both saves must happen — if member save is skipped, the project is orphaned
        verify(projectRepository, times(1)).save(any(Project.class));
        verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
        verify(auditLogger, times(1)).log("owner@test.com", "CREATE_PROJECT", 1L, null);
    }

    @Test
    void createProject_shouldThrow_whenMemberSaveFails() {
        // This test simulates the scenario @Transactional was added to prevent:
        // project saves successfully but member save throws → should roll back
        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Test Project")
                .build();

        Project savedProject = Project.builder()
                .id(1L).name("Test Project").ownerId("owner@test.com").status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenThrow(new RuntimeException("DB constraint violation"));

        // With @Transactional, this exception bubbles up and rolls back the project save
        assertThrows(RuntimeException.class, () -> projectService.createProject(request));
    }

    @Test
    void createProject_shouldThrow_whenUserIsNotAuthenticated() {
        SecurityContextHolder.clearContext(); // no auth

        ProjectRequestDTO request = ProjectRequestDTO.builder().name("Project").build();

        assertThrows(UnauthorizedException.class, () -> projectService.createProject(request));

        // Nothing should be persisted
        verify(projectRepository, never()).save(any());
        verify(projectMemberRepository, never()).save(any());
    }

    // ── updateProject tests ───────────────────────────────────────────────────

    @Test
    void updateProject_shouldUpdateNameAndDescription() {
        Project existing = Project.builder()
                .id(1L).name("Old").description("Old Desc")
                .ownerId("owner@test.com").status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(projectAccessService).validateAdmin(1L, "owner@test.com");
        when(projectRepository.save(any(Project.class))).thenReturn(existing);

        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("New Name").description("New Desc").build();

        ProjectResponseDTO response = projectService.updateProject(1L, request);

        assertNotNull(response);
        verify(projectRepository, times(1)).save(existing);
        verify(auditLogger, times(1)).log("owner@test.com", "UPDATE_PROJECT", 1L, null);
    }

    @Test
    void updateProject_shouldThrow_whenProjectNotFound() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> projectService.updateProject(999L,
                        ProjectRequestDTO.builder().name("x").build()));

        verify(projectRepository, never()).save(any());
    }

    // ── deleteProject tests ───────────────────────────────────────────────────

    @Test
    void deleteProject_shouldDeleteAndAudit() {
        Project project = Project.builder()
                .id(1L).name("Project").ownerId("owner@test.com").status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        doNothing().when(projectAccessService).validateAdmin(1L, "owner@test.com");
        doNothing().when(projectRepository).delete(project);

        projectService.deleteProject(1L);

        verify(projectRepository, times(1)).delete(project);
        verify(auditLogger, times(1)).log("owner@test.com", "DELETE_PROJECT", 1L, null);
    }

    @Test
    void deleteProject_shouldThrow_whenProjectNotFound() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.deleteProject(999L));

        verify(projectRepository, never()).delete(any());
    }

    // ── getProjectById tests ──────────────────────────────────────────────────

    @Test
    void getProjectById_shouldReturnProject_whenUserIsMember() {
        Project project = Project.builder()
                .id(1L).name("Project").ownerId("owner@test.com").status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectAccessService.validateMember(1L, "owner@test.com")).thenReturn(
                ProjectMember.builder().projectId(1L).userId("owner@test.com").role(ProjectRole.MEMBER).build()
        );

        ProjectResponseDTO response = projectService.getProjectById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getProjectById_shouldThrow_whenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectById(99L));
    }
}