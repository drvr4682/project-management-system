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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private AuthFeignClient authFeignClient;
    private ProjectFeignClient projectFeignClient;

    private TaskService taskService;

    // SETUP
    @BeforeEach
    void setup() {

        taskRepository = Mockito.mock(TaskRepository.class);
        authFeignClient = Mockito.mock(AuthFeignClient.class);
        projectFeignClient = Mockito.mock(ProjectFeignClient.class);

        taskService = new TaskServiceImpl(taskRepository, authFeignClient, projectFeignClient);

        // MOCK SECURITY CONTEXT (CRITICAL FIX)
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "admin@test.com",
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // CLEANUP
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateTask_asAdmin() {

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Task 1");
        request.setProjectId(1L);
        request.setAssignedTo("user@test.com");

        Task saved = Task.builder()
                .id(1L)
                .title("Task 1")
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
        
        Mockito.doNothing()
                .when(projectFeignClient)
                .validateAdmin(Mockito.anyLong());

        TaskResponseDTO response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals("Task 1", response.getTitle());
    }

    @Test
    void shouldFail_whenNotAdmin() {

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Task 1");
        request.setProjectId(1L);
        request.setAssignedTo("user@test.com");

        Mockito.when(authFeignClient.checkUser(Mockito.anyString()))
                .thenReturn("User exists");

        Mockito.when(projectFeignClient.getProject(Mockito.anyLong()))
                .thenReturn(new Object());

        Mockito.doThrow(feign.FeignException.Forbidden.class)
                .when(projectFeignClient)
                .validateAdmin(Mockito.anyLong());

        assertThrows(RuntimeException.class, () ->
                taskService.createTask(request)
        );
    }

    @Test
    void shouldUpdateStatus() {

        Task task = Task.builder()
                .id(1L)
                .title("Task")
                .projectId(1L)
                .assignedTo("user@test.com")
                .status(TaskStatus.TODO)
                .build();

        Mockito.when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        Mockito.when(taskRepository.save(Mockito.any(Task.class)))
                .thenReturn(task);

        Mockito.when(projectFeignClient.getProject(Mockito.anyLong()))
                .thenReturn(new Object());

        UpdateTaskStatusDTO dto = new UpdateTaskStatusDTO();
        dto.setStatus("DONE");

        TaskResponseDTO response = taskService.updateStatus(1L, dto);

        assertEquals("DONE", response.getStatus());
    }

    @Test
    void shouldFetchTasksWithPagination() {

        Mockito.when(projectFeignClient.getProject(Mockito.anyLong()))
                .thenReturn(new Object());

        org.springframework.data.domain.Page<Task> page =
                new org.springframework.data.domain.PageImpl<>(List.of(
                        Task.builder()
                                .id(1L)
                                .title("Task 1")
                                .projectId(1L)
                                .assignedTo("user@test.com")
                                .status(TaskStatus.TODO)
                                .build()
                ));

        Mockito.when(taskRepository.findByProjectId(Mockito.anyLong(), Mockito.any()))
                .thenReturn(page);

        var result = taskService.getTasksByProject(1L, 0, 5, null, "createdAt", "desc");

        assertEquals(1, result.getTotalElements());
    }
}