package com.pms.projectservice.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.projectservice.entity.Project;
import com.pms.projectservice.entity.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    Page<Project> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Project> findByStatusAndNameContainingIgnoreCase(
        ProjectStatus status,
        String name,
        Pageable pageable
    );

    Page<Project> findDistinctByIdIn(
            List<Long> projectIds,
            Pageable pageable
    );
}