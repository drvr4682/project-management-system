package com.pms.projectservice.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pms.projectservice.dto.AddMemberRequestDTO;
import com.pms.projectservice.service.ProjectMemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping("/{projectId}/members")
    public String addMember(
            @PathVariable Long projectId,
            @RequestBody AddMemberRequestDTO request) {

        return projectMemberService.addMember(projectId, request);
    } 
}
