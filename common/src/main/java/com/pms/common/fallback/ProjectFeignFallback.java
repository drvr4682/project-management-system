package com.pms.common.fallback;

import com.pms.common.client.ProjectFeignClient;
import com.pms.common.dto.ProjectSummaryDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Default fallback for ProjectFeignClient.
 *
 * Returns null for getProject() so callers receive a clear signal
 * that the project service was unreachable.
 * validateAdmin() throws to ensure admin checks are never silently skipped.
 */
@Slf4j
public class ProjectFeignFallback implements ProjectFeignClient {

    @Override
    public ProjectSummaryDTO getProject(Long projectId) {
        log.error("PROJECT SERVICE UNAVAILABLE — fallback triggered for projectId: {}", projectId);
        return null;
    }

    @Override
    public void validateAdmin(Long projectId) {
        log.error("PROJECT SERVICE UNAVAILABLE — admin validation fallback for projectId: {}", projectId);
        throw new RuntimeException("Project service is currently unavailable");
    }
}