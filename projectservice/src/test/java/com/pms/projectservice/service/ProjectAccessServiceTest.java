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

    private static final String USER_STR = "e5a31a61-9cbf-4bfb-b654-e67d4b9f36f1";
    private static final java.util.UUID USER_UUID = java.util.UUID.fromString(USER_STR);

    private static final String ADMIN_STR = "f8af7f79-8994-481e-99bf-2f78b498912c";
    private static final java.util.UUID ADMIN_UUID = java.util.UUID.fromString(ADMIN_STR);

    private static final String MEMBER_STR = "6fbe36c0-0381-45df-922e-e47bb37f3ad5";
    private static final java.util.UUID MEMBER_UUID = java.util.UUID.fromString(MEMBER_STR);

    private static final String MISSING_STR = "da6cd3a7-e17f-4702-861c-8ad621f3791a";
    private static final java.util.UUID MISSING_UUID = java.util.UUID.fromString(MISSING_STR);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should allow valid project member")
    void shouldAllowValidProjectMember() {

        ProjectMember member = ProjectMember.builder()
                .projectId(1L)
                .userId(USER_UUID)
                .role(ProjectRole.MEMBER)
                .build();

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, USER_UUID))
                .thenReturn(Optional.of(member));

        ProjectMember result =
                projectAccessService.validateMember(
                        1L,
                        USER_STR
                );

        assertNotNull(result);
        assertEquals(ProjectRole.MEMBER, result.getRole());
    }

    @Test
    @DisplayName("Should deny access when user is not project member")
    void shouldDenyAccessWhenUserNotProjectMember() {

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, MISSING_UUID))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.validateMember(
                        1L,
                        MISSING_STR
                )
        );
    }

    @Test
    @DisplayName("Should allow admin user")
    void shouldAllowAdminUser() {

        ProjectMember admin = ProjectMember.builder()
                .projectId(1L)
                .userId(ADMIN_UUID)
                .role(ProjectRole.ADMIN)
                .build();

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, ADMIN_UUID))
                .thenReturn(Optional.of(admin));

        assertDoesNotThrow(() ->
                projectAccessService.validateAdmin(
                        1L,
                        ADMIN_STR
                )
        );
    }

    @Test
    @DisplayName("Should deny non-admin user")
    void shouldDenyNonAdminUser() {

        ProjectMember member = ProjectMember.builder()
                .projectId(1L)
                .userId(MEMBER_UUID)
                .role(ProjectRole.MEMBER)
                .build();

        when(projectMemberRepository
                .findByProjectIdAndUserId(1L, MEMBER_UUID))
                .thenReturn(Optional.of(member));

        assertThrows(
                AccessDeniedException.class,
                () -> projectAccessService.validateAdmin(
                        1L,
                        MEMBER_STR
                )
        );
    }
}