package com.pms.projectservice.repository;

import com.pms.projectservice.entity.Project;
import com.pms.projectservice.entity.ProjectStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    private static final java.util.UUID OWNER_ID = java.util.UUID.randomUUID();

    @Test
    @DisplayName("Should save project successfully")
    void shouldSaveProjectSuccessfully() {

        Project project = Project.builder()
                .name("PMSHub Backend")
                .description("Backend system")
                .ownerId(OWNER_ID)
                .status(ProjectStatus.ACTIVE)
                .build();

        Project saved = projectRepository.save(project);

        assertNotNull(saved.getId());
        assertEquals("PMSHub Backend", saved.getName());
        assertEquals(ProjectStatus.ACTIVE, saved.getStatus());
    }

    @Test
    @DisplayName("Should find projects by status")
    void shouldFindProjectsByStatus() {

        Project activeProject = Project.builder()
                .name("Active Project")
                .description("Active Desc")
                .ownerId(OWNER_ID)
                .status(ProjectStatus.ACTIVE)
                .build();

        Project completedProject = Project.builder()
                .name("Completed Project")
                .description("Completed Desc")
                .ownerId(OWNER_ID)
                .status(ProjectStatus.COMPLETED)
                .build();

        projectRepository.saveAll(
                List.of(activeProject, completedProject)
        );

        Page<Project> result =
                projectRepository.findByStatus(
                        ProjectStatus.ACTIVE,
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(
                "Active Project",
                result.getContent().getFirst().getName()
        );
    }

    @Test
    @DisplayName("Should search projects by name")
    void shouldSearchProjectsByName() {

        Project project1 = Project.builder()
                .name("Task Management System")
                .description("Desc")
                .ownerId(OWNER_ID)
                .status(ProjectStatus.ACTIVE)
                .build();

        Project project2 = Project.builder()
                .name("Inventory System")
                .description("Desc")
                .ownerId(OWNER_ID)
                .status(ProjectStatus.ACTIVE)
                .build();

        projectRepository.saveAll(List.of(project1, project2));

        Page<Project> result =
                projectRepository.findByNameContainingIgnoreCase(
                        "task",
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(
                "Task Management System",
                result.getContent().getFirst().getName()
        );
    }

    @Test
    @DisplayName("Should filter by status and name")
    void shouldFilterByStatusAndName() {

        Project project1 = Project.builder()
                .name("Backend API")
                .description("Desc")
                .ownerId(OWNER_ID)
                .status(ProjectStatus.ACTIVE)
                .build();

        Project project2 = Project.builder()
                .name("Backend Admin")
                .description("Desc")
                .ownerId(OWNER_ID)
                .status(ProjectStatus.COMPLETED)
                .build();

        projectRepository.saveAll(List.of(project1, project2));

        Page<Project> result =
                projectRepository.findByStatusAndNameContainingIgnoreCase(
                        ProjectStatus.ACTIVE,
                        "backend",
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(
                ProjectStatus.ACTIVE,
                result.getContent().getFirst().getStatus()
        );
    }
}