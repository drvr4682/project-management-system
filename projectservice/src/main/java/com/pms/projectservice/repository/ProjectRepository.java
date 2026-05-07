package com.pms.projectservice.repository;

import com.pms.projectservice.entity.Project;
import com.pms.projectservice.entity.ProjectStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Project> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.status = :status AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Project> findByStatusAndNameContaining(
        @Param("status") ProjectStatus status,
        @Param("name") String name,
        Pageable pageable
    );
}