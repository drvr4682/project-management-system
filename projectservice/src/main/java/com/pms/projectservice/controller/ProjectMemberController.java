package com.pms.projectservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.dto.ProjectMemberResponseDTO;
import com.pms.projectservice.service.ProjectMemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping("/{projectId}/members")
    public ResponseEntity<String> addMember(
            @PathVariable Long projectId,
            @RequestBody AddMemberRequestDTO request) {

        return ResponseEntity.ok(projectMemberService.addMember(projectId, request));
    } 

    @GetMapping("/{projectId}/members")
    public List<ProjectMemberResponseDTO> getMembers(@PathVariable Long projectId) {
        return projectMemberService.getMembers(projectId);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public String removeMember(
            @PathVariable Long projectId,
            @PathVariable String userId) {

        return projectMemberService.removeMember(projectId, userId);
    }
}
