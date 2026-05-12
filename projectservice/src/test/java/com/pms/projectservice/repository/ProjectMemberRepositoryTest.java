package com.pms.projectservice.repository;

import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ProjectMemberRepositoryTest {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Test
    @DisplayName("Should save project member successfully")
    void shouldSaveProjectMemberSuccessfully() {

        ProjectMember member = ProjectMember.builder()
                .projectId(1L)
                .userId("user@test.com")
                .role(ProjectRole.ADMIN)
                .build();

        ProjectMember saved = projectMemberRepository.save(member);

        assertNotNull(saved.getId());
        assertEquals("user@test.com", saved.getUserId());
        assertEquals(ProjectRole.ADMIN, saved.getRole());
    }

    @Test
    @DisplayName("Should find member by projectId and userId")
    void shouldFindMemberByProjectIdAndUserId() {

        ProjectMember member = ProjectMember.builder()
                .projectId(1L)
                .userId("member@test.com")
                .role(ProjectRole.MEMBER)
                .build();

        projectMemberRepository.save(member);

        Optional<ProjectMember> found =
                projectMemberRepository.findByProjectIdAndUserId(
                        1L,
                        "member@test.com"
                );

        assertTrue(found.isPresent());
        assertEquals(
                ProjectRole.MEMBER,
                found.get().getRole()
        );
    }

    @Test
    @DisplayName("Should return all members for project")
    void shouldReturnAllMembersForProject() {

        ProjectMember member1 = ProjectMember.builder()
                .projectId(100L)
                .userId("admin@test.com")
                .role(ProjectRole.ADMIN)
                .build();

        ProjectMember member2 = ProjectMember.builder()
                .projectId(100L)
                .userId("user@test.com")
                .role(ProjectRole.MEMBER)
                .build();

        ProjectMember otherProjectMember = ProjectMember.builder()
                .projectId(200L)
                .userId("other@test.com")
                .role(ProjectRole.VIEWER)
                .build();

        projectMemberRepository.saveAll(
                List.of(member1, member2, otherProjectMember)
        );

        List<ProjectMember> members =
                projectMemberRepository.findByProjectId(100L);

        assertEquals(2, members.size());
    }

    @Test
    @DisplayName("Should return empty when member not found")
    void shouldReturnEmptyWhenMemberNotFound() {

        Optional<ProjectMember> result =
                projectMemberRepository.findByProjectIdAndUserId(
                        999L,
                        "missing@test.com"
                );

        assertTrue(result.isEmpty());
    }
}