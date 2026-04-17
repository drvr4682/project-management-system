package com.pms.taskservice.repository;

import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskStatus;
import com.pms.taskservice.repository.TaskRepository;

import org.springframework.data.domain.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void shouldSaveAndFetchByProjectId() {

        Task task = Task.builder()
                .title("Test Task")
                .projectId(1L)
                .assignedTo("user@test.com")
                .status(TaskStatus.TODO)
                .build();

        taskRepository.save(task);

        var page = taskRepository.findByProjectId(
                        1L,
                        org.springframework.data.domain.PageRequest.of(0, 10)
                );

        List<Task> tasks = page.getContent();

        assertFalse(tasks.isEmpty());
        assertEquals("Test Task", tasks.get(0).getTitle());
    }

    @Test
    void shouldFindByAssignedUser() {

        Task task = Task.builder()
                .title("Assigned Task")
                .projectId(2L)
                .assignedTo("user@test.com")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        taskRepository.save(task);

        List<Task> tasks = taskRepository.findByAssignedTo("user@test.com");

        assertEquals(1, tasks.size());
    }

    @Test
    void shouldFilterByStatus() {

        Task task = Task.builder()
                .title("Done Task")
                .projectId(3L)
                .assignedTo("user@test.com")
                .status(TaskStatus.DONE)
                .build();

        taskRepository.save(task);

        var page = taskRepository.findByProjectIdAndStatus(
                        3L,
                        TaskStatus.DONE,
                        org.springframework.data.domain.PageRequest.of(0, 10)
                );

        List<Task> tasks = page.getContent();

        assertEquals(1, tasks.size());
    }
}