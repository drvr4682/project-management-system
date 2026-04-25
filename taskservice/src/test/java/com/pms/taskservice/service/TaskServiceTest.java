package com.pms.taskservice.service;

import com.pms.common.client.AuthFeignClient;
import com.pms.common.client.ProjectFeignClient;
import com.pms.common.dto.ProjectSummaryDTO;
import com.pms.common.dto.UserExistsResponse;
import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.dto.TaskResponseDTO;
import com.pms.taskservice.dto.UpdateTaskStatusDTO;
import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskStatus;
import com.pms.taskservice.exception.AccessDeniedException;
import com.pms.taskservice.exception.ResourceNotFoundException;
import com.pms.taskservice.repository.TaskRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private AuthFeignClient authFeignClient;
    private ProjectFeignClient projectFeignClient;
    private TaskServiceImpl taskService;

    @BeforeEach
    void setup() {
        taskRepository = Mockito.mock(TaskRepository.class);
        authFeignClient = Mockito.mock(AuthFeignClient.class);
        projectFeignClient = Mockito.mock(ProjectFeignClient.class);

        taskService = new TaskServiceImpl(taskRepository, authFeignClient, projectFeignClient);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin@test.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateTask_whenUserAndProjectAreValid() {

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Task 1");
        request.setProjectId(1L);
        request.setAssignedTo("user@test.com");

        // Correct return type: UserExistsResponse (not String)
        when(authFeignClient.checkUser("user@test.com"))
                .thenReturn(UserExistsResponse.found("user@test.com"));

        // Correct return type: ProjectSummaryDTO (not Object)
        when(projectFeignClient.getProject(1L))
                .thenReturn(ProjectSummaryDTO.builder()
                        .id(1L)
                        .name("Test Project")
                        .status("ACTIVE")
                        .ownerId("admin@test.com")
                        .build());

        doNothing().when(projectFeignClient).validateAdmin(1L);

        Task saved = Task.builder()
                .id(1L)
                .title("Task 1")
                .projectId(1L)
                .assignedTo("user@test.com")
                .status(TaskStatus.TODO)
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponseDTO response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals("Task 1", response.getTitle());
        assertEquals("TODO", response.getStatus());
        assertEquals(1L, response.getProjectId());

        verify(authFeignClient, times(1)).checkUser("user@test.com");
        verify(projectFeignClient, times(1)).getProject(1L);
        verify(projectFeignClient, times(1)).validateAdmin(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldThrow_whenUserDoesNotExist() {

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Task 1");
        request.setProjectId(1L);
        request.setAssignedTo("ghost@test.com");

        when(authFeignClient.checkUser("ghost@test.com"))
                .thenReturn(UserExistsResponse.notFound("ghost@test.com"));

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));

        verify(projectFeignClient, never()).getProject(anyLong());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldThrow_whenUserIsNotProjectAdmin() {

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Task 1");
        request.setProjectId(1L);
        request.setAssignedTo("user@test.com");

        when(authFeignClient.checkUser("user@test.com"))
                .thenReturn(UserExistsResponse.found("user@test.com"));

        when(projectFeignClient.getProject(1L))
                .thenReturn(ProjectSummaryDTO.builder()
                        .id(1L).name("Project").status("ACTIVE").ownerId("other@test.com")
                        .build());

        doThrow(feign.FeignException.Forbidden.class)
                .when(projectFeignClient).validateAdmin(1L);

        assertThrows(AccessDeniedException.class, () -> taskService.createTask(request));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldUpdateTaskStatus() {

        Task task = Task.builder()
                .id(1L)
                .title("Task")
                .projectId(1L)
                .assignedTo("user@test.com")
                .status(TaskStatus.TODO)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        when(projectFeignClient.getProject(1L))
                .thenReturn(ProjectSummaryDTO.builder()
                        .id(1L).name("Project").status("ACTIVE").ownerId("admin@test.com")
                        .build());

        Task updatedTask = Task.builder()
                .id(1L).title("Task").projectId(1L)
                .assignedTo("user@test.com").status(TaskStatus.DONE)
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        UpdateTaskStatusDTO dto = new UpdateTaskStatusDTO();
        dto.setStatus("DONE");

        TaskResponseDTO response = taskService.updateStatus(1L, dto);

        assertEquals("DONE", response.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldThrow_whenTaskNotFound() {

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateTaskStatusDTO dto = new UpdateTaskStatusDTO();
        dto.setStatus("DONE");

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateStatus(999L, dto));
    }

    @Test
    void shouldFetchTasksByProject_withPagination() {

        when(projectFeignClient.getProject(1L))
                .thenReturn(ProjectSummaryDTO.builder()
                        .id(1L).name("Project").status("ACTIVE").ownerId("admin@test.com")
                        .build());

        Page<Task> taskPage = new PageImpl<>(List.of(
                Task.builder()
                        .id(1L).title("Task 1").projectId(1L)
                        .assignedTo("user@test.com").status(TaskStatus.TODO)
                        .build()
        ));

        when(taskRepository.findByProjectId(anyLong(), any(PageRequest.class)))
                .thenReturn(taskPage);

        Page<TaskResponseDTO> result = taskService.getTasksByProject(1L, 0, 5, null, "createdAt", "desc");

        assertEquals(1, result.getTotalElements());
        assertEquals("Task 1", result.getContent().get(0).getTitle());
    }
}