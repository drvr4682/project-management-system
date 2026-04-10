package com.pms.taskservice.service;

import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.dto.TaskResponseDTO;
import com.pms.taskservice.dto.UpdateTaskStatusDTO;
import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskStatus;
import com.pms.taskservice.repository.TaskRepository;
import com.pms.taskservice.client.AuthFeignClient;
import com.pms.taskservice.client.ProjectFeignClient;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private final TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
    private final AuthFeignClient authFeignClient = Mockito.mock(AuthFeignClient.class);
    private final ProjectFeignClient projectFeignClient = Mockito.mock(ProjectFeignClient.class);

    private final TaskService taskService = new TaskServiceImpl(taskRepository, authFeignClient, projectFeignClient);

    @Test
    void shouldCreateTask() {

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Test Task");
        request.setProjectId(1L);
        request.setAssignedTo("user@test.com");

        Task saved = Task.builder()
                .id(1L)
                .title("Test Task")
                .projectId(1L)
                .assignedTo("user@test.com")
                .status(TaskStatus.TODO)
                .build();

        Mockito.when(taskRepository.save(Mockito.any(Task.class)))
                .thenReturn(saved);

        Mockito.when(authFeignClient.checkUser(Mockito.anyString()))
                .thenReturn("User exists");

        Mockito.when(projectFeignClient.getProject(Mockito.anyLong()))
                .thenReturn(new Object());

        TaskResponseDTO response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals("Test Task", response.getTitle());
    }

    @Test
    void shouldUpdateStatus() {

        Task task = Task.builder()
                .id(1L)
                .title("Test")
                .projectId(1L)
                .assignedTo("user@test.com")
                .status(TaskStatus.TODO)
                .build();

        Mockito.when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        Mockito.when(taskRepository.save(Mockito.any(Task.class)))
                .thenReturn(task);

        UpdateTaskStatusDTO dto = new UpdateTaskStatusDTO();
        dto.setStatus("DONE");

        TaskResponseDTO response = taskService.updateStatus(1L, dto);

        assertEquals("DONE", response.getStatus());
    }
}