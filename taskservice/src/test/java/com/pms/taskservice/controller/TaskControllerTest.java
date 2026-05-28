package com.pms.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.filter.GatewayValidationFilter;
import com.pms.common.security.JwtUtil;
import com.pms.taskservice.dto.AssignTaskRequestDTO;
import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.dto.TaskResponseDTO;
import com.pms.taskservice.exception.AccessDeniedException;
import com.pms.taskservice.exception.ResourceNotFoundException;
import com.pms.taskservice.exception.ServiceUnavailableException;
import com.pms.taskservice.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TaskController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class TaskControllerTest {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final String BASE_URL          = "/api/v1/tasks";
    private static final Long   TASK_ID           = 1L;
    private static final Long   PROJECT_ID        = 42L;
    private static final String GATEWAY_SECRET_HDR = "X-Gateway-Secret";
    private static final String GATEWAY_SECRET_VAL = "test-gateway-secret";

    // -----------------------------------------------------------------------
    // Beans
    // -----------------------------------------------------------------------

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TaskService taskService;
    @MockBean JwtUtil     jwtUtil;   // required by JwtAuthenticationFilter in context

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private TaskResponseDTO buildResponse() {
        return TaskResponseDTO.builder()
                .id(TASK_ID)
                .title("Test Task")
                .description("A description")
                .status("TODO")
                .priority("MEDIUM")
                .projectId(PROJECT_ID)
                .createdBy("user@example.com")
                .build();
    }

    private TaskRequestDTO validRequest() {
        return TaskRequestDTO.builder()
                .title("Test Task")
                .description("A description")
                .status("TODO")
                .priority("MEDIUM")
                .projectId(PROJECT_ID)
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/tasks
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/tasks — createTask")
    class CreateTask {

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 201 with task body on success")
        void createTask_success_returns201() throws Exception {
            when(taskService.createTask(any(TaskRequestDTO.class)))
                    .thenReturn(buildResponse());

            mockMvc.perform(post(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(TASK_ID))
                    .andExpect(jsonPath("$.title").value("Test Task"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.priority").value("MEDIUM"))
                    .andExpect(jsonPath("$.projectId").value(PROJECT_ID))
                    .andExpect(jsonPath("$.createdBy").value("user@example.com"));

            verify(taskService).createTask(any(TaskRequestDTO.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
        @DisplayName("ADMIN role can also create a task")
        void createTask_asAdmin_returns201() throws Exception {
            when(taskService.createTask(any(TaskRequestDTO.class)))
                    .thenReturn(buildResponse());

            mockMvc.perform(post(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when title is blank")
        void createTask_blankTitle_returns400() throws Exception {
            TaskRequestDTO bad = TaskRequestDTO.builder()
                    .title("")          // @NotBlank violation
                    .projectId(PROJECT_ID)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.title").exists());

            verifyNoInteractions(taskService);
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when projectId is missing")
        void createTask_missingProjectId_returns400() throws Exception {
            TaskRequestDTO bad = TaskRequestDTO.builder()
                    .title("Valid Title")
                    // projectId intentionally omitted — @NotNull
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.projectId").exists());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when status value is invalid (pattern violation)")
        void createTask_invalidStatus_returns400() throws Exception {
            String body = """
                    {
                      "title": "Task",
                      "projectId": 42,
                      "status": "INVALID_STATUS"
                    }
                    """;

            mockMvc.perform(post(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.status").exists());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when priority value is invalid (pattern violation)")
        void createTask_invalidPriority_returns400() throws Exception {
            String body = """
                    {
                      "title": "Task",
                      "projectId": 42,
                      "priority": "EXTREME"
                    }
                    """;

            mockMvc.perform(post(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.priority").exists());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 503 when project service is unavailable")
        void createTask_serviceUnavailable_returns503() throws Exception {
            when(taskService.createTask(any()))
                    .thenThrow(new ServiceUnavailableException("Project service unavailable"));

            mockMvc.perform(post(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isServiceUnavailable());
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/tasks/{taskId}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/tasks/{taskId} — getTask")
    class GetTaskById {

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 200 with task body when found")
        void getTask_found_returns200() throws Exception {
            when(taskService.getTaskById(TASK_ID)).thenReturn(buildResponse());

            mockMvc.perform(get(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TASK_ID))
                    .andExpect(jsonPath("$.title").value("Test Task"));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 404 when task does not exist")
        void getTask_notFound_returns404() throws Exception {
            when(taskService.getTaskById(TASK_ID))
                    .thenThrow(new ResourceNotFoundException("Task not found with id: " + TASK_ID));

            mockMvc.perform(get(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Task not found with id: " + TASK_ID));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 403 when user is not a project member")
        void getTask_notMember_returns403() throws Exception {
            when(taskService.getTaskById(TASK_ID))
                    .thenThrow(new AccessDeniedException("User is not a member"));

            mockMvc.perform(get(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/tasks?projectId=...
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/tasks — getTasks (paginated)")
    class GetTasks {

        @BeforeEach
        void stubService() {
            Page<TaskResponseDTO> page = new PageImpl<>(
                    List.of(buildResponse()),
                    PageRequest.of(0, 10),
                    1
            );
            when(taskService.getTasks(
                    anyLong(), any(), any(), any(), any(),
                    anyInt(), anyInt(), anyString(), anyString()))
                    .thenReturn(page);
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 200 with page of tasks (no filters)")
        void getTasks_noFilters_returns200() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .param("projectId", String.valueOf(PROJECT_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(TASK_ID))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 200 with status and priority filters applied")
        void getTasks_withFilters_returns200() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .param("projectId", String.valueOf(PROJECT_ID))
                            .param("status", "TODO")
                            .param("priority", "MEDIUM")
                            .param("search", "Test")
                            .param("page", "0")
                            .param("size", "5")
                            .param("sortBy", "createdAt")
                            .param("direction", "desc"))
                    .andExpect(status().isOk());

            verify(taskService).getTasks(
                    eq(PROJECT_ID), eq("TODO"), eq("MEDIUM"),
                    isNull(), eq("Test"),
                    eq(0), eq(5), eq("createdAt"), eq("desc"));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when page is negative")
        void getTasks_negativePage_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .param("projectId", String.valueOf(PROJECT_ID))
                            .param("page", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when size is zero")
        void getTasks_sizeZero_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .param("projectId", String.valueOf(PROJECT_ID))
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when size exceeds 100")
        void getTasks_sizeTooLarge_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .param("projectId", String.valueOf(PROJECT_ID))
                            .param("size", "101"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 200 with assignedTo filter")
        void getTasks_withAssignedTo_returns200() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .param("projectId", String.valueOf(PROJECT_ID))
                            .param("assignedTo", "dev@example.com"))
                    .andExpect(status().isOk());

            verify(taskService).getTasks(
                    eq(PROJECT_ID), isNull(), isNull(),
                    eq("dev@example.com"), isNull(),
                    eq(0), eq(10), eq("createdAt"), eq("desc"));
        }
    }

    // -----------------------------------------------------------------------
    // PUT /api/v1/tasks/{taskId}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/tasks/{taskId} — updateTask")
    class UpdateTask {

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 200 with updated task on success")
        void updateTask_success_returns200() throws Exception {
            TaskResponseDTO updated = buildResponse();
            updated.setTitle("Updated Title");
            when(taskService.updateTask(eq(TASK_ID), any(TaskRequestDTO.class))).thenReturn(updated);

            TaskRequestDTO request = validRequest();
            request.setTitle("Updated Title");

            mockMvc.perform(put(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 404 when task to update does not exist")
        void updateTask_notFound_returns404() throws Exception {
            when(taskService.updateTask(eq(TASK_ID), any()))
                    .thenThrow(new ResourceNotFoundException("Task not found with id: " + TASK_ID));

            mockMvc.perform(put(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 403 when user lacks edit permission")
        void updateTask_forbidden_returns403() throws Exception {
            when(taskService.updateTask(eq(TASK_ID), any()))
                    .thenThrow(new AccessDeniedException("Only the task creator or a project admin can perform this action"));

            mockMvc.perform(put(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when request body fails validation")
        void updateTask_invalidBody_returns400() throws Exception {
            TaskRequestDTO bad = TaskRequestDTO.builder()
                    .title("")           // blank — @NotBlank
                    .projectId(PROJECT_ID)
                    .build();

            mockMvc.perform(put(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/tasks/{taskId}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{taskId} — deleteTask")
    class DeleteTask {

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 200 with confirmation message on success")
        void deleteTask_success_returns200() throws Exception {
            doNothing().when(taskService).deleteTask(TASK_ID);

            mockMvc.perform(delete(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Task deleted successfully"));

            verify(taskService).deleteTask(TASK_ID);
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 404 when task to delete does not exist")
        void deleteTask_notFound_returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Task not found with id: " + TASK_ID))
                    .when(taskService).deleteTask(TASK_ID);

            mockMvc.perform(delete(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 403 when user is not creator or admin")
        void deleteTask_forbidden_returns403() throws Exception {
            doThrow(new AccessDeniedException("Only the task creator or a project admin can perform this action"))
                    .when(taskService).deleteTask(TASK_ID);

            mockMvc.perform(delete(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // PUT /api/v1/tasks/{taskId}/assign
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/tasks/{taskId}/assign — assignTask")
    class AssignTask {

        private AssignTaskRequestDTO assignRequest(String uuid) {
            AssignTaskRequestDTO req = new AssignTaskRequestDTO();
            req.setAssigneeId(uuid);
            return req;
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 200 with assigned task on success")
        void assignTask_success_returns200() throws Exception {
            String targetUuid = "f8af7f79-8994-481e-99bf-2f78b498912c";
            TaskResponseDTO response = buildResponse();
            response.setAssignedTo(targetUuid);
            when(taskService.assignTask(eq(TASK_ID), any(AssignTaskRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(assignRequest(targetUuid))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedTo").value(targetUuid));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when assigneeId is blank")
        void assignTask_blankAssignee_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"assigneeId\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.assigneeId").exists());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when assigneeId is not a valid UUID")
        void assignTask_invalidUuid_returns400() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"assigneeId\": \"not-a-uuid\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.assigneeId").exists());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 400 when assignee does not exist in auth service")
        void assignTask_assigneeNotFound_returns400() throws Exception {
            String ghostUuid = "6fbe36c0-0381-45df-922e-e47bb37f3ad5";
            when(taskService.assignTask(eq(TASK_ID), any()))
                    .thenThrow(new IllegalArgumentException("Assignee user does not exist"));

            mockMvc.perform(put(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(assignRequest(ghostUuid))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Assignee user does not exist"));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 404 when task does not exist")
        void assignTask_taskNotFound_returns404() throws Exception {
            String targetUuid = "f8af7f79-8994-481e-99bf-2f78b498912c";
            when(taskService.assignTask(eq(TASK_ID), any()))
                    .thenThrow(new ResourceNotFoundException("Task not found with id: " + TASK_ID));

            mockMvc.perform(put(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(assignRequest(targetUuid))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 403 when user lacks permission to assign")
        void assignTask_forbidden_returns403() throws Exception {
            String targetUuid = "f8af7f79-8994-481e-99bf-2f78b498912c";
            when(taskService.assignTask(eq(TASK_ID), any()))
                    .thenThrow(new AccessDeniedException("Only the task creator or a project admin can perform this action"));

            mockMvc.perform(put(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(assignRequest(targetUuid))))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/tasks/{taskId}/assign
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{taskId}/assign — removeAssignee")
    class RemoveAssignee {

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 200 with task body (assignedTo = null) on success")
        void removeAssignee_success_returns200() throws Exception {
            TaskResponseDTO response = buildResponse();
            response.setAssignedTo(null);
            when(taskService.removeAssignee(TASK_ID)).thenReturn(response);

            mockMvc.perform(delete(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedTo").doesNotExist());

            verify(taskService).removeAssignee(TASK_ID);
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 404 when task does not exist")
        void removeAssignee_taskNotFound_returns404() throws Exception {
            when(taskService.removeAssignee(TASK_ID))
                    .thenThrow(new ResourceNotFoundException("Task not found with id: " + TASK_ID));

            mockMvc.perform(delete(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = {"USER"})
        @DisplayName("Returns 403 when user lacks permission to remove assignee")
        void removeAssignee_forbidden_returns403() throws Exception {
            when(taskService.removeAssignee(TASK_ID))
                    .thenThrow(new AccessDeniedException("Only the task creator or a project admin can perform this action"));

            mockMvc.perform(delete(BASE_URL + "/{taskId}/assign", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, GATEWAY_SECRET_VAL))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // Gateway filter
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GatewayValidationFilter integration")
    class GatewayValidationFilterTest {

        private MockMvc filteredMvc;

        @BeforeEach
        void setUpWithFilter() {
            GatewayValidationFilter filter =
                    new GatewayValidationFilter(GATEWAY_SECRET_VAL, objectMapper);
            filteredMvc = MockMvcBuilders
                    .standaloneSetup(new TaskController(taskService))
                    .addFilter(filter)
                    .build();
        }

        @Test
        @DisplayName("Returns 403 when X-Gateway-Secret header is missing")
        void noGatewaySecret_returns403() throws Exception {
            filteredMvc.perform(get(BASE_URL + "/{taskId}", TASK_ID))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(taskService);
        }

        @Test
        @DisplayName("Returns 403 when X-Gateway-Secret value is wrong")
        void wrongGatewaySecret_returns403() throws Exception {
            filteredMvc.perform(get(BASE_URL + "/{taskId}", TASK_ID)
                            .header(GATEWAY_SECRET_HDR, "wrong-secret"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(taskService);
        }
    }
}