package com.pms.taskservice.repository;

import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByAssignedTo(String assignedTo);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
}