package com.pms.projectservice.service;

import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.AccessDeniedException;
import com.pms.projectservice.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectMemberRepository projectMemberRepository;

    @Transactional(readOnly = true)
    public ProjectMember validateMember(Long projectId, String userId) {

        java.util.UUID userUuid;
        try {
            userUuid = java.util.UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for user: {}", userId);
            throw new AccessDeniedException("User not part of project");
        }

        return projectMemberRepository
                .findByProjectIdAndUserId(projectId, userUuid)
                .orElseThrow(() -> {

                    log.warn(
                            "Access denied. User {} is not a member of project {}",
                            userId,
                            projectId
                    );

                    return new AccessDeniedException("User not part of project");
                });
    }

    @Transactional(readOnly = true)
    public void validateAdmin(Long projectId, String userId) {

        ProjectMember member = validateMember(projectId, userId);

        if (member.getRole() != ProjectRole.ADMIN) {

            log.warn(
                    "Admin access denied. User {} tried admin action on project {}",
                    userId,
                    projectId
            );

            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
    }
}