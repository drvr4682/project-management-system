package com.pms.projectservice.service;

import static com.pms.projectservice.security.JwtFilter.currentUser;

import org.springframework.stereotype.Service;

import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.AccessDeniedException;
import com.pms.projectservice.exception.UnauthorizedException;
import com.pms.projectservice.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;

    public String addMember(Long projectId, AddMemberRequestDTO request) {

        String user = currentUser.get();

        if (user == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        // ✅ Check ADMIN access
        ProjectMember existing = projectMemberRepository
                .findByProjectIdAndUserId(projectId, user)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (existing.getRole() != ProjectRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can add members");
        }

        // ✅ Prevent duplicate
        projectMemberRepository.findByProjectIdAndUserId(projectId, request.getUserId())
                .ifPresent(m -> {
                    throw new IllegalArgumentException("User already a member");
                });

        // ✅ Role parsing
        ProjectRole role;
        try {
            role = ProjectRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role");
        }

        ProjectMember member = ProjectMember.builder()
                .projectId(projectId)
                .userId(request.getUserId())
                .role(role)
                .build();

        projectMemberRepository.save(member);

        log.info("User {} added to project {} as {}", request.getUserId(), projectId, role);

        return "Member added successfully";
    }    
}
