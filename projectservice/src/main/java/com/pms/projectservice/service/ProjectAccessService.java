package com.pms.projectservice.service;

import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.AccessDeniedException;
import com.pms.projectservice.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Both methods are read-only — they only query the DB.
     * readOnly = true improves performance by skipping dirty checking.
     * These are called from within @Transactional methods in other services,
     * so they participate in the caller's transaction via REQUIRED propagation (default).
     */
    @Transactional(readOnly = true)
    public ProjectMember validateMember(Long projectId, String userId) {
        return projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("User not part of project"));
    }

    @Transactional(readOnly = true)
    public void validateAdmin(Long projectId, String userId) {
        ProjectMember member = validateMember(projectId, userId);
        if (member.getRole() != ProjectRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
    }
}