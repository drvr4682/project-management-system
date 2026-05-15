package com.pms.projectservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pms.projectservice.entity.ProjectMember;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, String userId);
    List<ProjectMember> findByProjectId(Long projectId);
    List<ProjectMember> findByUserId(String userId);
    void deleteByProjectId(Long projectId);
}