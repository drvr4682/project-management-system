package com.pms.projectservice.service;

import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.AccessDeniedException;
import com.pms.projectservice.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMember validateMember(Long projectId, String userId) {
        return projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() ->
                        new AccessDeniedException("User not part of project"));
    }

    public void validateAdmin(Long projectId, String userId) {
        ProjectMember member = validateMember(projectId, userId);

        if (member.getRole() != ProjectRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can perform this action");
        }
    }
}