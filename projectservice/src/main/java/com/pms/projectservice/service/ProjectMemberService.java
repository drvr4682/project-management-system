package com.pms.projectservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.projectservice.client.AuthFeignClient;
import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.dto.ProjectMemberResponseDTO;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.ResourceNotFoundException;
import com.pms.projectservice.exception.ServiceUnavailableException;
import com.pms.projectservice.exception.UnauthorizedException;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.security.SecurityUtils;
import com.pms.projectservice.util.AuditLogger;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final AuthFeignClient authFeignClient;
    private final ProjectAccessService projectAccessService;
    private final AuditLogger auditLogger;
    private final SecurityUtils securityUtils;

    @Transactional
    public String addMember(Long projectId, AddMemberRequestDTO request) {

        String currentUser = securityUtils.getCurrentUser();

        log.info(
                "Add Member Request | User: {} | ProjectId: {} | TargetUser: {} | CorrelationId: {}",
                currentUser,
                projectId,
                request.getUserId(),
                securityUtils.getCorrelationId()
        );

        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        projectAccessService.validateAdmin(projectId, currentUser);

        projectMemberRepository.findByProjectIdAndUserId(projectId, request.getUserId())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("User already a member");
                });

        ProjectRole role;

        try {

            role = ProjectRole.valueOf(
                    request.getRole().toUpperCase()
            );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid project role"
            );
        }

        String response = validateUserFromAuthService(request.getUserId());

        if (!"User exists".equalsIgnoreCase(response)) {

            throw new IllegalArgumentException("User does not exist");
        }

        ProjectMember member = ProjectMember.builder()
                .projectId(projectId)
                .userId(request.getUserId())
                .role(role)
                .build();

        projectMemberRepository.save(member);

        auditLogger.log(currentUser, "ADD_MEMBER", projectId, request.getUserId());

        log.info("User {} added to project {} as {}", request.getUserId(), projectId, role);

        return "Member added successfully";
    }

    @CircuitBreaker(
            name = "authService",
            fallbackMethod = "validateUserFallback"
    )
    @Retry(name = "authService")
    public String validateUserFromAuthService(
            String userId
    ) {

        log.info(
                "Calling AuthService to validate user: {}",
                userId
        );

        return authFeignClient.checkUser(userId);
    }

    public String validateUserFallback(
            String userId,
            Exception exception
    ) {

        log.error(
                "AuthService fallback triggered for user: {} | Error: {}",
                userId,
                exception.getMessage()
        );

        throw new ServiceUnavailableException(
                "Auth service unavailable"
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponseDTO> getMembers(Long projectId) {

        String currentUser = securityUtils.getCurrentUser();

        log.info(
                "Get Members Request | User: {} | ProjectId: {} | CorrelationId: {}",
                currentUser,
                projectId,
                securityUtils.getCorrelationId()
        );

        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        projectAccessService.validateMember(projectId, currentUser);

        return projectMemberRepository.findByProjectId(projectId)
                .stream()
                .map(m -> ProjectMemberResponseDTO.builder()
                        .userId(m.getUserId())
                        .role(m.getRole().name())
                        .build())
                .toList();
    }

    @Transactional
    public String removeMember(Long projectId, String userId) {

        String currentUser = securityUtils.getCurrentUser();

        log.info(
                "Remove Member Request | User: {} | ProjectId: {} | TargetUser: {} | CorrelationId: {}",
                currentUser,
                projectId,
                userId,
                securityUtils.getCorrelationId()
        );

        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        projectAccessService.validateAdmin(projectId, currentUser);

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (currentUser.equals(userId)) {
            throw new IllegalArgumentException(
                    "Project admin cannot remove themselves"
            );
        }
        projectMemberRepository.delete(member);

        auditLogger.log(currentUser, "REMOVE_MEMBER", projectId, userId);

        return "Member removed successfully";
    }
}