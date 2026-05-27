package com.pms.projectservice.service;

import com.pms.common.security.SecurityUtils;
import com.pms.projectservice.client.AuthFeignClient;
import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.dto.ProjectMemberResponseDTO;
import com.pms.projectservice.entity.Project;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.entity.ProjectStatus;
import com.pms.projectservice.exception.ResourceNotFoundException;
import com.pms.projectservice.exception.ServiceUnavailableException;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.repository.ProjectRepository;
import com.pms.projectservice.util.AuditLogger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuthFeignClient authFeignClient;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AuthValidationComponent authValidationComponent;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private static final String ADMIN_STR = "e5a31a61-9cbf-4bfb-b654-e67d4b9f36f1";
    private static final java.util.UUID ADMIN_UUID = java.util.UUID.fromString(ADMIN_STR);

    private static final String MEMBER_STR = "f8af7f79-8994-481e-99bf-2f78b498912c";
    private static final java.util.UUID MEMBER_UUID = java.util.UUID.fromString(MEMBER_STR);

    private static final String MISSING_STR = "6fbe36c0-0381-45df-922e-e47bb37f3ad5";
    private static final java.util.UUID MISSING_UUID = java.util.UUID.fromString(MISSING_STR);

    private final Project stubProject = Project.builder()
            .id(1L)
            .name("Test Project")
            .ownerId(ADMIN_UUID)
            .status(ProjectStatus.ACTIVE)
            .build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(securityUtils.getCurrentUser()).thenReturn(ADMIN_STR);
        when(securityUtils.getCorrelationId()).thenReturn("corr-123");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(stubProject));
        when(authValidationComponent.validateUser(anyString())).thenReturn("User exists");
    }

    @Test
    @DisplayName("Should add member successfully")
    void shouldAddMemberSuccessfully() {
        AddMemberRequestDTO request = new AddMemberRequestDTO();
        request.setUserId(MEMBER_STR);
        request.setRole("MEMBER");

        doNothing().when(projectAccessService).validateAdmin(1L, ADMIN_STR);

        when(projectMemberRepository.findByProjectIdAndUserId(1L, MEMBER_UUID))
                .thenReturn(Optional.empty());

        when(authFeignClient.checkUser(MEMBER_STR)).thenReturn("User exists");

        String response = projectMemberService.addMember(1L, request);

        assertEquals("Member added successfully", response);

        verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
        verify(auditLogger, times(1))
                .log(ADMIN_STR, "ADD_MEMBER", 1L, MEMBER_STR);
    }

    @Test
    @DisplayName("Should throw exception when member already exists")
    void shouldThrowExceptionWhenMemberAlreadyExists() {
        AddMemberRequestDTO request = new AddMemberRequestDTO();
        request.setUserId(MEMBER_STR);
        request.setRole("MEMBER");

        doNothing().when(projectAccessService).validateAdmin(1L, ADMIN_STR);

        when(projectMemberRepository.findByProjectIdAndUserId(1L, MEMBER_UUID))
                .thenReturn(Optional.of(ProjectMember.builder()
                        .id(1L)
                        .projectId(1L)
                        .userId(MEMBER_UUID)
                        .role(ProjectRole.MEMBER)
                        .build()));

        assertThrows(IllegalArgumentException.class,
                () -> projectMemberService.addMember(1L, request));
    }

    @Test
    @DisplayName("Should return all members")
    void shouldReturnAllMembers() {
        when(projectAccessService.validateMember(1L, ADMIN_STR))
                .thenReturn(ProjectMember.builder()
                        .projectId(1L)
                        .userId(ADMIN_UUID)
                        .role(ProjectRole.ADMIN)
                        .build());

        List<ProjectMember> members = List.of(
                ProjectMember.builder().userId(ADMIN_UUID).role(ProjectRole.ADMIN).build(),
                ProjectMember.builder().userId(MEMBER_UUID).role(ProjectRole.MEMBER).build()
        );

        when(projectMemberRepository.findByProjectId(1L)).thenReturn(members);

        List<ProjectMemberResponseDTO> result = projectMemberService.getMembers(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should remove member successfully")
    void shouldRemoveMemberSuccessfully() {
        ProjectMember member = ProjectMember.builder()
                .id(1L).projectId(1L).userId(MEMBER_UUID).role(ProjectRole.MEMBER).build();

        doNothing().when(projectAccessService).validateAdmin(1L, ADMIN_STR);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, MEMBER_UUID))
                .thenReturn(Optional.of(member));

        String response = projectMemberService.removeMember(1L, MEMBER_STR);

        assertEquals("Member removed successfully", response);
        verify(projectMemberRepository, times(1)).delete(member);
    }

    @Test
    @DisplayName("Should throw exception when member not found on remove")
    void shouldThrowExceptionWhenMemberNotFound() {
        doNothing().when(projectAccessService).validateAdmin(1L, ADMIN_STR);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, MISSING_UUID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> projectMemberService.removeMember(1L, MISSING_STR));
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException when AuthService is unavailable")
    void shouldThrowServiceUnavailableWhenAuthServiceUnavailable() {
        AddMemberRequestDTO request = new AddMemberRequestDTO();
        request.setUserId(MEMBER_STR);
        request.setRole("MEMBER");

        doNothing().when(projectAccessService).validateAdmin(1L, ADMIN_STR);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, MEMBER_UUID))
                .thenReturn(Optional.empty());

        // ✅ Throw exception from mocked component
        when(authValidationComponent.validateUser(MEMBER_STR))
                .thenThrow(new ServiceUnavailableException("Auth service unavailable"));

        assertThrows(ServiceUnavailableException.class,
                () -> projectMemberService.addMember(1L, request));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid project role")
    void shouldThrowExceptionForInvalidProjectRole() {
        AddMemberRequestDTO request = new AddMemberRequestDTO();
        request.setUserId(MEMBER_STR);
        request.setRole("SUPER_ADMIN");

        doNothing().when(projectAccessService).validateAdmin(1L, ADMIN_STR);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, MEMBER_UUID))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> projectMemberService.addMember(1L, request));

        assertEquals("Invalid project role", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when project does not exist")
    void shouldThrowNotFoundWhenProjectDoesNotExist() {
        AddMemberRequestDTO request = new AddMemberRequestDTO();
        request.setUserId(MEMBER_STR);
        request.setRole("MEMBER");

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> projectMemberService.addMember(999L, request));
    }
}