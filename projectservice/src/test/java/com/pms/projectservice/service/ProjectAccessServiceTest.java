package com.pms.projectservice.service;

import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.AccessDeniedException;
import com.pms.projectservice.repository.ProjectMemberRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ProjectAccessServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAccessService projectAccessService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should allow valid project member")
    void shouldAllowValidProjectMember() {

        ProjectMember member = ProjectMember.builder()
                .projectId(1L)
                .userId("user@test.com")
                .role(ProjectRole.MEMBER)
                .build();

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "user@test.com"))
                .thenReturn(Optional.of(member));

        ProjectMember result =
                projectAccessService.validateMember(
                        1L,
                        "user@test.com"
                );

        assertNotNull(result);
        assertEquals(ProjectRole.MEMBER, result.getRole());
    }

    @Test
    @DisplayName("Should deny access when user is not project member")
    void shouldDenyAccessWhenUserNotProjectMember() {

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "missing@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.validateMember(
                        1L,
                        "missing@test.com"
                )
        );
    }

    @Test
    @DisplayName("Should allow admin user")
    void shouldAllowAdminUser() {

        ProjectMember admin = ProjectMember.builder()
                .projectId(1L)
                .userId("admin@test.com")
                .role(ProjectRole.ADMIN)
                .build();

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "admin@test.com"))
                .thenReturn(Optional.of(admin));

        assertDoesNotThrow(() ->
                projectAccessService.validateAdmin(
                        1L,
                        "admin@test.com"
                )
        );
    }

    @Test
    @DisplayName("Should deny non-admin user")
    void shouldDenyNonAdminUser() {

        ProjectMember member = ProjectMember.builder()
                .projectId(1L)
                .userId("member@test.com")
                .role(ProjectRole.MEMBER)
                .build();

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, "member@test.com"))
                .thenReturn(Optional.of(member));

        assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.validateAdmin(
                        1L,
                        "member@test.com"
                )
        );
    }
}