package com.pms.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.taskservice.dto.TaskRequestDTO;
import com.pms.taskservice.service.TaskService;
import com.pms.taskservice.security.JwtAuthenticationFilter;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldCreateTask() throws Exception {

        TaskRequestDTO request = new TaskRequestDTO();
        request.setTitle("Test Task");
        request.setProjectId(1L);
        request.setAssignedTo("user@test.com");

        Mockito.when(taskService.createTask(Mockito.any()))
                .thenReturn(null);

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailWhenInvalidInput() throws Exception {

        TaskRequestDTO request = new TaskRequestDTO();

        mockMvc.perform(post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}