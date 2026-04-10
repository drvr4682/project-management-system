package com.pms.taskservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "project-service",
        url = "http://localhost:8082",
        fallback = com.pms.taskservice.client.fallback.ProjectFeignFallback.class
)
public interface ProjectFeignClient {

    @GetMapping("/api/v1/projects/{projectId}")
    Object getProject(@PathVariable Long projectId);
}