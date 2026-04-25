package com.pms.taskservice.client;

import com.pms.common.client.ProjectFeignClient;
import com.pms.common.dto.ProjectSummaryDTO;
import com.pms.taskservice.exception.ServiceUnavailableException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
public class ProjectFeignFallback implements ProjectFeignClient {

    @Override
    public ProjectSummaryDTO getProject(Long projectId) {
        log.error("Project service unavailable — fallback for projectId: {}", projectId);
        throw new ServiceUnavailableException("Project service is currently unavailable");
    }

    @Override
    public void validateAdmin(Long projectId) {
        log.error("Project service unavailable — admin validation fallback for projectId: {}", projectId);
        throw new ServiceUnavailableException("Project service is currently unavailable");
    }
}