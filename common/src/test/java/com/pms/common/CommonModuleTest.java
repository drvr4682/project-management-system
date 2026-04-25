package com.pms.common;

import com.pms.common.dto.ProjectSummaryDTO;
import com.pms.common.dto.UserExistsResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonModuleTest {

    @Test
    void userExistsResponse_found_shouldSetExistsTrue() {
        UserExistsResponse response = UserExistsResponse.found("test@example.com");

        assertTrue(response.isExists());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void userExistsResponse_notFound_shouldSetExistsFalse() {
        UserExistsResponse response = UserExistsResponse.notFound("missing@example.com");

        assertFalse(response.isExists());
        assertEquals("missing@example.com", response.getEmail());
    }

    @Test
    void projectSummaryDTO_shouldBuildCorrectly() {
        ProjectSummaryDTO dto = ProjectSummaryDTO.builder()
                .id(1L)
                .name("Test Project")
                .status("ACTIVE")
                .ownerId("owner@example.com")
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("Test Project", dto.getName());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals("owner@example.com", dto.getOwnerId());
    }

    @Test
    void userExistsResponse_defaultConstructor_shouldWork() {
        UserExistsResponse response = new UserExistsResponse();
        response.setEmail("a@b.com");
        response.setExists(true);

        assertTrue(response.isExists());
        assertEquals("a@b.com", response.getEmail());
    }
}