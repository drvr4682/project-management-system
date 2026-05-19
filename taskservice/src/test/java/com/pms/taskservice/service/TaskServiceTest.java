package com.pms.taskservice.service;

import com.pms.taskservice.dto.AssignTaskRequestDTO;
import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.dto.TaskResponseDTO;
import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskPriority;
import com.pms.taskservice.entity.TaskStatus;
import com.pms.taskservice.exception.AccessDeniedException;
import com.pms.taskservice.exception.ResourceNotFoundException;
import com.pms.taskservice.exception.UnauthorizedException;
import com.pms.taskservice.repository.TaskRepository;
import com.pms.taskservice.security.SecurityUtils;
import com.pms.taskservice.util.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectValidationComponent projectValidationComponent;
    @Mock private AuthValidationComponent authValidationComponent;
    @Mock private SecurityUtils securityUtils;
    @Mock private AuditLogger auditLogger;

    @InjectMocks
    private TaskService taskService;

    private static final String CURRENT_USER = "user@example.com";
    private static final Long   PROJECT_ID   = 1L;
    private static final Long   TASK_ID      = 10L;

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUser()).thenReturn(CURRENT_USER);
        when(securityUtils.getCorrelationId()).thenReturn("test-correlation-id");
    }

    // ------------------------------------------------------------------ //
    //  createTask
    // ------------------------------------------------------------------ //

    @Test
    void createTask_success() {

        TaskRequestDTO request = TaskRequestDTO.builder()
                .title("New Task")
                .description("desc")
                .projectId(PROJECT_ID)
                .status("TODO")
                .priority("HIGH")
                .build();

        Task saved = buildTask(TASK_ID, "New Task", CURRENT_USER);
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponseDTO response = taskService.createTask(request);

        assertThat(response.getId()).isEqualTo(TASK_ID);
        assertThat(response.getTitle()).isEqualTo("New Task");
        verify(projectValidationComponent).validateProjectMember(PROJECT_ID);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_unauthenticated_throws() {
        when(securityUtils.getCurrentUser()).thenReturn(null);

        assertThatThrownBy(() -> taskService.createTask(
                TaskRequestDTO.builder().title("t").projectId(PROJECT_ID).build()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void createTask_invalidStatus_throws() {
        TaskRequestDTO request = TaskRequestDTO.builder()
                .title("Task")
                .projectId(PROJECT_ID)
                .status("INVALID_STATUS")
                .build();

        // status validation is done in service via valueOf — bypass @Pattern which is controller-level
        // The @Pattern annotation only fires with @Valid in controllers.
        // Here we just confirm the service handles a raw invalid string gracefully.
        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }

    // ------------------------------------------------------------------ //
    //  getTaskById
    // ------------------------------------------------------------------ //

    @Test
    void getTaskById_success() {
        Task task = buildTask(TASK_ID, "My Task", CURRENT_USER);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        TaskResponseDTO response = taskService.getTaskById(TASK_ID);

        assertThat(response.getId()).isEqualTo(TASK_ID);
        verify(projectValidationComponent).validateProjectMember(PROJECT_ID);
    }

    @Test
    void getTaskById_notFound_throws() {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(TASK_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------ //
    //  updateTask
    // ------------------------------------------------------------------ //

    @Test
    void updateTask_byCreator_success() {
        Task task = buildTask(TASK_ID, "Old Title", CURRENT_USER);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskRequestDTO request = TaskRequestDTO.builder()
                .title("New Title")
                .projectId(PROJECT_ID)
                .build();

        TaskResponseDTO response = taskService.updateTask(TASK_ID, request);

        assertThat(response.getTitle()).isEqualTo("New Title");
    }

    @Test
    void updateTask_byNonCreatorNonAdmin_throws() {
        Task task = buildTask(TASK_ID, "Title", "other@example.com");
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        // non-creator, not admin — validateProjectAdmin throws AccessDeniedException
        doThrow(new AccessDeniedException("not admin"))
                .when(projectValidationComponent).validateProjectAdmin(PROJECT_ID);

        TaskRequestDTO request = TaskRequestDTO.builder()
                .title("Hacked Title")
                .projectId(PROJECT_ID)
                .build();

        assertThatThrownBy(() -> taskService.updateTask(TASK_ID, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------------------------ //
    //  deleteTask
    // ------------------------------------------------------------------ //

    @Test
    void deleteTask_byCreator_success() {
        Task task = buildTask(TASK_ID, "Title", CURRENT_USER);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        taskService.deleteTask(TASK_ID);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_notFound_throws() {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(TASK_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------ //
    //  assignTask
    // ------------------------------------------------------------------ //

    @Test
    void assignTask_success() {
        Task task = buildTask(TASK_ID, "Title", CURRENT_USER);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(authValidationComponent.validateUser("assignee@example.com"))
                .thenReturn("User exists");
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        AssignTaskRequestDTO request = new AssignTaskRequestDTO();
        request.setAssigneeId("assignee@example.com");

        TaskResponseDTO response = taskService.assignTask(TASK_ID, request);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void assignTask_assigneeNotFound_throws() {
        Task task = buildTask(TASK_ID, "Title", CURRENT_USER);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(authValidationComponent.validateUser("ghost@example.com"))
                .thenReturn("not found"); // simulate unexpected response

        AssignTaskRequestDTO request = new AssignTaskRequestDTO();
        request.setAssigneeId("ghost@example.com");

        assertThatThrownBy(() -> taskService.assignTask(TASK_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
    }

    // ------------------------------------------------------------------ //
    //  removeAssignee
    // ------------------------------------------------------------------ //

    @Test
    void removeAssignee_success() {
        Task task = buildTask(TASK_ID, "Title", CURRENT_USER);
        task.setAssignedTo("assignee@example.com");
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponseDTO response = taskService.removeAssignee(TASK_ID);

        assertThat(response).isNotNull();
        verify(taskRepository).save(argThat(t -> t.getAssignedTo() == null));
    }

    // ------------------------------------------------------------------ //
    //  helper
    // ------------------------------------------------------------------ //

    private Task buildTask(Long id, String title, String createdBy) {
        return Task.builder()
                .id(id)
                .title(title)
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .projectId(PROJECT_ID)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
