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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final AuthFeignClient authFeignClient;
    private final ProjectAccessService projectAccessService;
    private final AuditLogger auditLogger;

    public String addMember(Long projectId, AddMemberRequestDTO request) {

        String currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) throw new UnauthorizedException("Unauthorized");

        projectAccessService.validateAdmin(projectId, currentUser);

        projectMemberRepository.findByProjectIdAndUserId(projectId, request.getUserId())
                .ifPresent(m -> { throw new IllegalArgumentException("User already a member"); });

        ProjectRole role;
        try {
            role = ProjectRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }

        // Delegate to resilience-wrapped method
        validateUserExists(request.getUserId());

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

    @CircuitBreaker(name = "auth-service", fallbackMethod = "userValidationFallback")
    @Retry(name = "auth-service")
    public void validateUserExists(String email) {
        try {
            UserExistsResponse response = authFeignClient.checkUser(email);
            if (!response.isExists()) {
                throw new IllegalArgumentException("User does not exist: " + email);
            }
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("User does not exist: " + email);
        }
    }

    public void userValidationFallback(String email, Throwable t) {
        log.error("Auth circuit OPEN — email: {}, cause: {}", email, t.getMessage());
        throw new ServiceUnavailableException("Auth service is currently unavailable");
    }

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