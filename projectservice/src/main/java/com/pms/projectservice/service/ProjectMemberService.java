package com.pms.projectservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.pms.projectservice.client.AuthFeignClient;
import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.dto.ProjectMemberResponseDTO;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.ResourceNotFoundException;
import com.pms.projectservice.exception.UnauthorizedException;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.security.SecurityUtils;
import com.pms.projectservice.util.AuditLogger;

import feign.FeignException;
import feign.RetryableException;
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

    public String addMember(Long projectId, AddMemberRequestDTO request) {

        String currentUser = SecurityUtils.getCurrentUser();

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
            role = ProjectRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role");
        }

        try {

            String response = authFeignClient.checkUser(request.getUserId());

            if(!"User exists".equalsIgnoreCase(response)) {
                throw new IllegalArgumentException("User does not exist");
            }
            
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("User does not exist");

        } catch (RetryableException e) {
            throw new IllegalArgumentException("Auth service unavailable");

        } catch (FeignException e) {
            throw new IllegalArgumentException("Error calling auth service");
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

    public List<ProjectMemberResponseDTO> getMembers(Long projectId) {

        String currentUser = SecurityUtils.getCurrentUser();

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

    public String removeMember(Long projectId, String userId) {

        String currentUser = SecurityUtils.getCurrentUser();

        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        projectAccessService.validateAdmin(projectId, currentUser);

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        projectMemberRepository.delete(member);

        auditLogger.log(currentUser, "REMOVE_MEMBER", projectId, userId);

        return "Member removed successfully";
    }
}