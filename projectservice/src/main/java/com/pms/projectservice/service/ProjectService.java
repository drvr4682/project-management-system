package com.pms.projectservice.service;

import com.pms.projectservice.dto.*;
import com.pms.projectservice.entity.Project;
import com.pms.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public String healthCheck() {
        return "Project Service is running";
    }

    public ProjectResponseDTO createProject(ProjectRequestDTO request) {

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner("TEMP_USER") // will be replaced by JWT later
                .build();

        Project saved = projectRepository.save(project);

        return ProjectResponseDTO.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .owner(saved.getOwner())
                .build();
    }
}