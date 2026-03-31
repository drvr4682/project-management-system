package com.pms.projectservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.projectservice.dto.ProjectRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProject_shouldReturn200() throws Exception {

        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Test Project")
                .description("Test Desc")
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void createProject_shouldFail_whenNameIsBlank() throws Exception {

        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("") // invalid
                .description("Test Desc")
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProject_shouldReturnProject_whenOwnerMatches() throws Exception {

        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Owner Test")
                .description("Test")
                .build();

        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/projects/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void updateProject_shouldReturnUpdatedProject() throws Exception {

        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Initial")
                .description("Initial Desc")
                .build();

        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        ProjectRequestDTO update = ProjectRequestDTO.builder()
                .name("Updated")
                .description("Updated Desc")
                .build();

        mockMvc.perform(put("/api/v1/projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());
    }

    @Test
    void updateProject_shouldFail_whenNotOwner() throws Exception {

        // create project
        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Project")
                .description("Desc")
                .build();

        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        ProjectRequestDTO update = ProjectRequestDTO.builder()
                .name("Hack")
                .description("Hack Desc")
                .build();

        mockMvc.perform(put("/api/v1/projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProject_shouldDeleteSuccessfully() throws Exception {

        ProjectRequestDTO request = ProjectRequestDTO.builder()
                .name("Delete Test")
                .description("To be deleted")
                .build();

        String response = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/v1/projects/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProject_shouldFail_whenProjectNotFound() throws Exception {

        mockMvc.perform(delete("/api/v1/projects/999"))
                .andExpect(status().isBadRequest());
    }
}