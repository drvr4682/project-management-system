package com.pms.common.client;

import com.pms.common.dto.ProjectSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client interface for Project Service.
 *
 * Used by Task Service to verify project existence and admin status.
 * Returns ProjectSummaryDTO — a lean internal contract, not the full
 * public API response.
 */
@FeignClient(
    name = "project-service",
    url = "${services.project.url}"
)
public interface ProjectFeignClient {

    @GetMapping("/api/v1/projects/{projectId}")
    ProjectSummaryDTO getProject(@PathVariable Long projectId);

    @GetMapping("/api/v1/projects/{projectId}/validate-admin")
    void validateAdmin(@PathVariable Long projectId);
}