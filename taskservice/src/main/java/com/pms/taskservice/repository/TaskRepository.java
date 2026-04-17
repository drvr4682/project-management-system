package com.pms.taskservice.repository;

import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    List<Task> findByAssignedTo(String assignedTo);

    Page<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status, Pageable pageable);
}