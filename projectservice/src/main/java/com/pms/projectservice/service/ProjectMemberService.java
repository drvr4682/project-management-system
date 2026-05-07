package com.pms.projectservice.service;

import com.pms.common.client.AuthFeignClient;
import com.pms.common.dto.UserExistsResponse;
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

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final AuthFeignClient authFeignClient;
    private final ProjectAccessService projectAccessService;
    private final AuditLogger auditLogger;

    /**
     * addMember is @Transactional on the DB work only.
     *
     * IMPORTANT: validateUserExists() is NOT inside this transaction
     * because it makes a Feign (HTTP) call. Keeping HTTP calls outside
     * DB transactions prevents long-held connections and connection pool exhaustion.
     *
     * Flow:
     *   1. validateUserExists() — HTTP call, outside transaction
     *   2. save(member) + auditLogger — inside @Transactional below
     *
     * We split the method into: addMember (orchestrates) → persistMember (transactional DB work).
     */
    public String addMember(Long projectId, AddMemberRequestDTO request) {
        String currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) throw new UnauthorizedException("Unauthorized");

        // Access check — reads the DB, not inside a long transaction
        projectAccessService.validateAdmin(projectId, currentUser);

        // Duplicate check
        projectMemberRepository.findByProjectIdAndUserId(projectId, request.getUserId())
                .ifPresent(m -> { throw new IllegalArgumentException("User already a member"); });

        ProjectRole role;
        try {
            role = ProjectRole.valueOf(request.getRole().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole(), e);
        }

        // HTTP call — deliberately OUTSIDE the DB transaction
        validateUserExists(request.getUserId());

        // DB write — in its own short transaction
        persistMember(projectId, request.getUserId(), role, currentUser);

        return "Member added successfully";
    }

    /**
     * All DB writes for addMember happen here, in a single short transaction.
     * If the save fails, nothing is committed.
     */
    @Transactional
    protected void persistMember(Long projectId, String userId, ProjectRole role, String currentUser) {
        ProjectMember member = ProjectMember.builder()
                .projectId(projectId)
                .userId(userId)
                .role(role)
                .build();

        projectMemberRepository.save(member);
        auditLogger.log(currentUser, "ADD_MEMBER", projectId, userId);
        log.info("User {} added to project {} as {}", userId, projectId, role);
    }

    /**
     * Resilience-wrapped HTTP call — no @Transactional here.
     * Circuit breaker and retry operate on the HTTP call, not a DB transaction.
     */
    @CircuitBreaker(name = "auth-service", fallbackMethod = "userValidationFallback")
    @Retry(name = "auth-service")
    public void validateUserExists(String email) {
        try {
            UserExistsResponse response = authFeignClient.checkUser(email);
            if (!response.isExists()) {
                throw new IllegalArgumentException("User does not exist: " + email);
            }
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("User does not exist: " + email, e);
        }
    }

    public void userValidationFallback(String email, Throwable t) {
        log.error("Auth circuit OPEN — email: {}, cause: {}", email, t.getMessage());
        throw new ServiceUnavailableException("Auth service is currently unavailable");
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponseDTO> getMembers(Long projectId) {
        String currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) throw new UnauthorizedException("Unauthorized");

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
        String currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) throw new UnauthorizedException("Unauthorized");

        projectAccessService.validateAdmin(projectId, currentUser);

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        projectMemberRepository.delete(member);
        auditLogger.log(currentUser, "REMOVE_MEMBER", projectId, userId);

        return "Member removed successfully";
    }
}