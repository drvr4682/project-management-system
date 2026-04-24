package com.pms.taskservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.pms.taskservice.client.fallback.ProjectFeignFallback;
import com.pms.taskservice.dto.ProjectResponseDTO;

@FeignClient(
    name = "project-service",
    url = "${services.project.url}",
    fallback = ProjectFeignFallback.class
)
public interface ProjectFeignClient {

    @GetMapping("/api/v1/projects/{projectId}")
    ProjectResponseDTO getProject(@PathVariable Long projectId);

    @GetMapping("/api/v1/projects/{projectId}/validate-admin")
    void validateAdmin(@PathVariable Long projectId);
}