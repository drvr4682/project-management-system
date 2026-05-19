package com.pms.taskservice.service;

import com.pms.taskservice.client.ProjectFeignClient;
import com.pms.taskservice.exception.AccessDeniedException;
import com.pms.taskservice.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectValidationComponent {

    private final ProjectFeignClient projectFeignClient;

    /**
     * Validates that the current user is a member of the given project.
     * The JWT is forwarded via FeignConfig interceptor — ProjectService enforces RBAC.
     */
    @CircuitBreaker(name = "projectService", fallbackMethod = "validateMemberFallback")
    @Retry(name = "projectService")
    public void validateProjectMember(Long projectId) {
        log.info("Calling ProjectService to validate membership for project: {}", projectId);
        projectFeignClient.getProject(projectId);
    }

    public void validateMemberFallback(Long projectId, Throwable throwable) {
        log.error(
                "ProjectService fallback triggered for project: {} | Error: {}",
                projectId, throwable.getMessage()
        );

        if (throwable instanceof IllegalArgumentException iae) {
            throw iae;
        }

        if (throwable instanceof RuntimeException re
                && re.getMessage() != null
                && re.getMessage().contains("Downstream auth error")) {
            throw new AccessDeniedException("User is not a member of project: " + projectId);
        }

        throw new ServiceUnavailableException(
                "Project service unavailable: " + throwable.getMessage()
        );
    }

    /**
     * Validates that the current user is an ADMIN of the given project.
     */
    @CircuitBreaker(name = "projectService", fallbackMethod = "validateAdminFallback")
    @Retry(name = "projectService")
    public void validateProjectAdmin(Long projectId) {
        log.info("Calling ProjectService to validate admin role for project: {}", projectId);
        projectFeignClient.validateAdmin(projectId);
    }

    public void validateAdminFallback(Long projectId, Throwable throwable) {
        log.error(
                "ProjectService admin-validation fallback for project: {} | Error: {}",
                projectId, throwable.getMessage()
        );

        if (throwable instanceof IllegalArgumentException iae) {
            throw iae;
        }

        if (throwable instanceof RuntimeException re
                && re.getMessage() != null
                && re.getMessage().contains("Downstream auth error")) {
            throw new AccessDeniedException("User is not an ADMIN of project: " + projectId);
        }

        throw new ServiceUnavailableException(
                "Project service unavailable: " + throwable.getMessage()
        );
    }
}
