package com.pms.projectservice.repository;

import com.pms.projectservice.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, String userId);
    List<ProjectMember> findByProjectId(Long projectId);

}