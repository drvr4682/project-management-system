package com.pms.projectservice.service;

import static com.pms.projectservice.security.JwtFilter.currentUser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pms.projectservice.client.AuthFeignClient;
import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.dto.ProjectMemberResponseDTO;
import com.pms.projectservice.entity.ProjectMember;
import com.pms.projectservice.entity.ProjectRole;
import com.pms.projectservice.exception.AccessDeniedException;
import com.pms.projectservice.exception.ResourceNotFoundException;
import com.pms.projectservice.exception.UnauthorizedException;
import com.pms.projectservice.repository.ProjectMemberRepository;

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

    public String addMember(Long projectId, AddMemberRequestDTO request) {

        String user = currentUser.get();

        if (user == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        ProjectMember existing = projectMemberRepository
                .findByProjectIdAndUserId(projectId, user)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (existing.getRole() != ProjectRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can add members");
        }

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
            String encodedEmail = URLEncoder.encode(request.getUserId(), StandardCharsets.UTF_8);

            authFeignClient.checkUser(encodedEmail);
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

        log.info("User {} added to project {} as {}", request.getUserId(), projectId, role);

        return "Member added successfully";
    }

    public List<ProjectMemberResponseDTO> getMembers(Long projectId) {

        String user = currentUser.get();

        if (user == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        // Must be a member
        projectMemberRepository.findByProjectIdAndUserId(projectId, user)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        return projectMemberRepository.findAll()
                .stream()
                .filter(m -> m.getProjectId().equals(projectId))
                .map(m -> ProjectMemberResponseDTO.builder()
                        .userId(m.getUserId())
                        .role(m.getRole().name())
                        .build())
                .toList();
    }

    public String removeMember(Long projectId, String userId) {

        String current = currentUser.get();

        if (current == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        // Check ADMIN
        ProjectMember admin = projectMemberRepository
                .findByProjectIdAndUserId(projectId, current)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (admin.getRole() != ProjectRole.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can remove members");
        }

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        projectMemberRepository.delete(member);

        return "Member removed successfully";
    }

    public ProjectMember validateMember(Long projectId, String userId) {
        return projectMemberRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied: Not a project member"));
    }

    public void validateAdmin(Long projectId, String userId) {
        ProjectMember member = validateMember(projectId, userId);

        if (!member.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("Access denied: Admin only");
        }
    }
}