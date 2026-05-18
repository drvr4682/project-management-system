package com.pms.projectservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.dto.ProjectMemberResponseDTO;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.ResourceNotFoundException;
import com.pms.projectservice.exception.UnauthorizedException;
import com.pms.projectservice.repository.ProjectMemberRepository;
import com.pms.projectservice.repository.ProjectRepository;
import com.pms.projectservice.security.SecurityUtils;
import com.pms.projectservice.util.AuditLogger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogger auditLogger;
    private final SecurityUtils securityUtils;
    private final AuthValidationComponent authValidationComponent;

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

        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId
                ));

        projectAccessService.validateAdmin(projectId, currentUser);

        projectMemberRepository.findByProjectIdAndUserId(projectId, request.getUserId())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("User already a member");
                });

        ProjectRole role;
        try {
            role = ProjectRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid project role");
        }

        String authResponse = authValidationComponent.validateUser(request.getUserId());


        if (!"User exists".equalsIgnoreCase(authResponse)) {
            throw new IllegalArgumentException("User does not exist");
        }

        ProjectMember member = ProjectMember.builder()
                .projectId(projectId)
                .userId(request.getUserId())
                .role(role)
                .build();

        projectMemberRepository.save(member);

        auditLogger.log(currentUser, "ADD_MEMBER", projectId, request.getUserId());

        log.info(
                "User {} added to project {} as {}",
                request.getUserId(),
                projectId,
                role
        );

        return "Member added successfully";
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