package com.pms.projectservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.security.JwtUtil;
import com.pms.projectservice.dto.ProjectRequestDTO;
import com.pms.projectservice.dto.ProjectResponseDTO;
import com.pms.projectservice.service.ProjectService;

@WebMvcTest(
        controllers = ProjectController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName("Should create project successfully")
    void shouldCreateProjectSuccessfully() throws Exception {

        ProjectRequestDTO request =
                new ProjectRequestDTO();

        request.setName("Backend API");
        request.setDescription("Project Desc");
        request.setStatus("ACTIVE");

        ProjectResponseDTO response =
                new ProjectResponseDTO();

        response.setId(1L);
        response.setName("Backend API");
        response.setDescription("Project Desc");
        response.setStatus("ACTIVE");

        when(projectService.createProject(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/projects")
                                .header("X-Gateway-Secret", "test-gateway-secret")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Backend API"));
    }

    @Test
    @WithMockUser(
            username = "user@test.com",
            roles = {"USER"}
    )
    @DisplayName("Should get all projects")
    void shouldGetAllProjects() throws Exception {

        ProjectResponseDTO response =
                new ProjectResponseDTO();

        response.setId(1L);
        response.setName("Backend API");
        response.setDescription("Desc");
        response.setStatus("ACTIVE");

        when(projectService.getProjects(
                any(),
                any(),
                any(Integer.class),
                any(Integer.class),
                any(),
                any()
        )).thenReturn(
                new PageImpl<>(List.of(response))
        );

        mockMvc.perform(
                        get("/api/v1/projects")
                .header("X-Gateway-Secret", "test-gateway-secret")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name")
                        .value("Backend API"));
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = {"ADMIN"}
    )
    @DisplayName("Should validate request body")
    void shouldValidateRequestBody() throws Exception {

        ProjectRequestDTO request =
                new ProjectRequestDTO();

        request.setName("");
        request.setDescription("Desc");
        request.setStatus("ACTIVE");

        mockMvc.perform(
                        post("/api/v1/projects")
                                .header("X-Gateway-Secret", "test-gateway-secret")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest());
    }
}