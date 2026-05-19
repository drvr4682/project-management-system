package com.pms.taskservice.repository;

import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskPriority;
import com.pms.taskservice.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    private static final Long PROJECT_ID = 1L;
    private static final String USER_A   = "usera@example.com";
    private static final String USER_B   = "userb@example.com";

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        taskRepository.save(task("Task Alpha", TaskStatus.TODO,       TaskPriority.HIGH,   USER_A));
        taskRepository.save(task("Task Beta",  TaskStatus.IN_PROGRESS,TaskPriority.MEDIUM, USER_A));
        taskRepository.save(task("Task Gamma", TaskStatus.DONE,       TaskPriority.LOW,    USER_B));
        taskRepository.save(task("Other Work", TaskStatus.TODO,       TaskPriority.CRITICAL, null));
    }

    private Task task(String title, TaskStatus status, TaskPriority priority, String assignedTo) {
        return Task.builder()
                .title(title)
                .status(status)
                .priority(priority)
                .projectId(PROJECT_ID)
                .createdBy(USER_A)
                .assignedTo(assignedTo)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Pageable page() {
        return PageRequest.of(0, 10);
    }

    @Test
    void findByProjectId_returnsAllProjectTasks() {
        Page<Task> result = taskRepository.findByProjectId(PROJECT_ID, page());
        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void findByProjectIdAndStatus_filtersByStatus() {
        Page<Task> result = taskRepository.findByProjectIdAndStatus(
                PROJECT_ID, TaskStatus.TODO, page());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByProjectIdAndPriority_filtersByPriority() {
        Page<Task> result = taskRepository.findByProjectIdAndPriority(
                PROJECT_ID, TaskPriority.HIGH, page());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Task Alpha");
    }

    @Test
    void findByProjectIdAndAssignedTo_filtersByAssignee() {
        Page<Task> result = taskRepository.findByProjectIdAndAssignedTo(
                PROJECT_ID, USER_A, page());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByProjectIdAndTitleContainingIgnoreCase_searchWorks() {
        Page<Task> result = taskRepository
                .findByProjectIdAndTitleContainingIgnoreCase(PROJECT_ID, "task", page());
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findByProjectIdAndStatusAndPriority_combinedFilter() {
        Page<Task> result = taskRepository
                .findByProjectIdAndStatusAndPriority(
                        PROJECT_ID, TaskStatus.TODO, TaskPriority.HIGH, page());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Task Alpha");
    }

    @Test
    void findByProjectIdAndStatusAndTitleContaining_combinedFilter() {
        Page<Task> result = taskRepository
                .findByProjectIdAndStatusAndTitleContainingIgnoreCase(
                        PROJECT_ID, TaskStatus.TODO, "Task", page());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Task Alpha");
    }

    @Test
    void findByProjectIdAndStatusAndPriorityAndTitle_allFilters() {
        Page<Task> result = taskRepository
                .findByProjectIdAndStatusAndPriorityAndTitleContainingIgnoreCase(
                        PROJECT_ID, TaskStatus.TODO, TaskPriority.HIGH, "alpha", page());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deleteByProjectId_removesAllProjectTasks() {
        taskRepository.deleteByProjectId(PROJECT_ID);
        assertThat(taskRepository.findByProjectId(PROJECT_ID, page()).getTotalElements()).isZero();
    }
}
