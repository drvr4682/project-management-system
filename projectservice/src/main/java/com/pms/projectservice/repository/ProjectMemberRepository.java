package com.pms.projectservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.projectservice.entity.ProjectMember;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, java.util.UUID userId);
    List<ProjectMember> findByProjectId(Long projectId);
    List<ProjectMember> findByUserId(java.util.UUID userId);
    void deleteByProjectId(Long projectId);
}