package com.pms.projectservice.service;

import com.pms.projectservice.client.AuthFeignClient;
import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.dto.ProjectMemberResponseDTO;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.ResourceNotFoundException;
import com.pms.projectservice.exception.ServiceUnavailableException;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.security.SecurityUtils;
import com.pms.projectservice.util.AuditLogger;

import feign.RetryableException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private AuthFeignClient authFeignClient;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        when(securityUtils.getCurrentUser())
                .thenReturn("admin@test.com");

        when(securityUtils.getCorrelationId())
                .thenReturn("corr-123");

        ReflectionTestUtils.setField(
                projectMemberService,
                "internalSecret",
                "test-internal-secret"
        );
    }

    @Test
    @DisplayName("Should add member successfully")
    void shouldAddMemberSuccessfully() {

        AddMemberRequestDTO request = new AddMemberRequestDTO();
        request.setUserId("member@test.com");
        request.setRole("MEMBER");

        doNothing().when(projectAccessService)
                .validateAdmin(1L, "admin@test.com");

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "member@test.com"))
                .thenReturn(Optional.empty());

        when(authFeignClient.checkUser("member@test.com", "test-internal-secret"))
                .thenReturn("User exists");

        String response =
                projectMemberService.addMember(1L, request);

        assertEquals("Member added successfully", response);

        verify(projectMemberRepository, times(1))
                .save(any(ProjectMember.class));

        verify(auditLogger, times(1))
                .log(
                        "admin@test.com",
                        "ADD_MEMBER",
                        1L,
                        "member@test.com"
                );
    }

    @Test
    @DisplayName("Should throw exception when member already exists")
    void shouldThrowExceptionWhenMemberAlreadyExists() {

        AddMemberRequestDTO request = new AddMemberRequestDTO();
        request.setUserId("member@test.com");
        request.setRole("MEMBER");

        doNothing().when(projectAccessService)
                .validateAdmin(1L, "admin@test.com");

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "member@test.com"))
                .thenReturn(
                        Optional.of(
                                ProjectMember.builder()
                                        .id(1L)
                                        .projectId(1L)
                                        .userId("member@test.com")
                                        .role(ProjectRole.MEMBER)
                                        .build()
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> projectMemberService.addMember(1L, request)
        );
    }

    @Test
    @DisplayName("Should return all members")
    void shouldReturnAllMembers() {

        when(projectAccessService
                .validateMember(1L, "admin@test.com"))
                .thenReturn(
                        ProjectMember.builder()
                                .projectId(1L)
                                .userId("admin@test.com")
                                .role(ProjectRole.ADMIN)
                                .build()
                );

        List<ProjectMember> members = List.of(
                ProjectMember.builder()
                        .userId("admin@test.com")
                        .role(ProjectRole.ADMIN)
                        .build(),

                ProjectMember.builder()
                        .userId("member@test.com")
                        .role(ProjectRole.MEMBER)
                        .build()
        );

        when(projectMemberRepository.findByProjectId(1L))
                .thenReturn(members);

        List<ProjectMemberResponseDTO> result =
                projectMemberService.getMembers(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should remove member successfully")
    void shouldRemoveMemberSuccessfully() {

        ProjectMember member = ProjectMember.builder()
                .id(1L)
                .projectId(1L)
                .userId("member@test.com")
                .role(ProjectRole.MEMBER)
                .build();

        doNothing().when(projectAccessService)
                .validateAdmin(1L, "admin@test.com");

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "member@test.com"))
                .thenReturn(Optional.of(member));

        String response =
                projectMemberService.removeMember(
                        1L,
                        "member@test.com"
                );

        assertEquals("Member removed successfully", response);

        verify(projectMemberRepository, times(1))
                .delete(member);
    }

    @Test
    @DisplayName("Should throw exception when member not found")
    void shouldThrowExceptionWhenMemberNotFound() {

        doNothing().when(projectAccessService)
                .validateAdmin(1L, "admin@test.com");

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "missing@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> projectMemberService.removeMember(
                        1L,
                        "missing@test.com"
                )
        );
    }

    @Test
    @DisplayName("Should throw exception when auth service unavailable")
    void shouldThrowExceptionWhenAuthServiceUnavailable() {

        AddMemberRequestDTO request = new AddMemberRequestDTO();
        request.setUserId("member@test.com");
        request.setRole("MEMBER");

        doNothing().when(projectAccessService)
                .validateAdmin(1L, "admin@test.com");

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "member@test.com"))
                .thenReturn(Optional.empty());

        when(authFeignClient.checkUser("member@test.com", "test-internal-secret"))
                .thenThrow(
                        mock(RetryableException.class)
                );

        assertThrows(
                ServiceUnavailableException.class,
                () -> projectMemberService.addMember(1L, request)
        );
    }
    @Test
    @DisplayName("Should throw exception for invalid project role")
    void shouldThrowExceptionForInvalidProjectRole() {

        AddMemberRequestDTO request =
                new AddMemberRequestDTO();

        request.setUserId("member@test.com");
        request.setRole("SUPER_ADMIN");

        doNothing().when(projectAccessService)
                .validateAdmin(1L, "admin@test.com");

        when(projectMemberRepository
                .findByProjectIdAndUserId(
                        1L,
                        "member@test.com"
                ))
                .thenReturn(Optional.empty());

        when(authFeignClient.checkUser("member@test.com", "test-internal-secret"))
                .thenReturn("User exists");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> projectMemberService.addMember(
                                1L,
                                request
                        )
                );

        assertEquals(
                "Invalid project role",
                exception.getMessage()
        );
    }
}