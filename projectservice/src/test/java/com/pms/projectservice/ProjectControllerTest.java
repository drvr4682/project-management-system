package com.pms.projectservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.projectservice.dto.ProjectRequestDTO;
import com.pms.projectservice.service.ProjectService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.pms.projectservice.controller.ProjectController;
import com.pms.projectservice.dto.ProjectResponseDTO;
import org.mockito.Mockito;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @Test
    void createProject_shouldReturn200() throws Exception {
        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Test Project")
                .description("Test Desc")
                .build();

        Mockito.when(projectService.createProject(Mockito.any()))
                .thenReturn(ProjectResponseDTO.builder()
                        .id(1L)
                        .name("Test Project")
                        .status("ACTIVE")
                        .build());

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void createProject_shouldFail_whenNameIsBlank() throws Exception {
        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("")
                .description("Test Desc")
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProject_shouldReturn200() throws Exception {
        Mockito.when(projectService.getProjectById(1L))
                .thenReturn(ProjectResponseDTO.builder()
                        .id(1L)
                        .name("Test Project")
                        .status("ACTIVE")
                        .build());

        mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProject_shouldReturn200() throws Exception {
        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Updated")
                .description("Updated Desc")
                .build();

        Mockito.when(projectService.updateProject(Mockito.eq(1L), Mockito.any()))
                .thenReturn(ProjectResponseDTO.builder()
                        .id(1L)
                        .name("Updated")
                        .status("ACTIVE")
                        .build());

        mockMvc.perform(put("/api/v1/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProject_shouldReturn200() throws Exception {
        Mockito.doNothing().when(projectService).deleteProject(1L);

        mockMvc.perform(delete("/api/v1/projects/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProject_shouldFail_whenProjectNotFound() throws Exception {
        Mockito.doThrow(new com.pms.projectservice.exception.ResourceNotFoundException("Project not found"))
                .when(projectService).deleteProject(999L);

        mockMvc.perform(delete("/api/v1/projects/999"))
                .andExpect(status().isNotFound());
    }
}