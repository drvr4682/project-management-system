package com.pms.taskservice.client.fallback;

import com.pms.taskservice.client.ProjectFeignClient;
import org.springframework.stereotype.Component;

@Component
public class ProjectFeignFallback implements ProjectFeignClient {

    @Override
    public Object getProject(Long projectId) {
        throw new RuntimeException("Project service unavailable");
    }

    @Override
    public void validateAdmin(Long projectId) {
        throw new RuntimeException("Project service unavailable");
    }
}