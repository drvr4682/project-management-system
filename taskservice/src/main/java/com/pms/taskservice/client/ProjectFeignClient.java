package com.pms.taskservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "project-service",
    url = "${services.project.url}"
)
public interface ProjectFeignClient {

    /**
     * Validates that the current JWT user is a member of the project.
     * Returns 200 OK if valid; 403/404 otherwise (handled by FeignErrorDecoder).
     */
    @GetMapping("/api/v1/projects/{projectId}")
    String getProject(@PathVariable Long projectId);

    /**
     * Validates that the current JWT user is an ADMIN of the project.
     * Returns 200 OK if valid; 403/404 otherwise.
     */
    @GetMapping("/api/v1/projects/{projectId}/validate-admin")
    Void validateAdmin(@PathVariable Long projectId);
}
