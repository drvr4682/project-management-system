package com.pms.taskservice.repository;

import com.pms.taskservice.entity.Task;
import com.pms.taskservice.entity.TaskPriority;
import com.pms.taskservice.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // --- All tasks in a project (base) ---
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    // --- Filter by status only ---
    Page<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status, Pageable pageable);

    // --- Filter by priority only ---
    Page<Task> findByProjectIdAndPriority(Long projectId, TaskPriority priority, Pageable pageable);

    // --- Filter by assignee only ---
    Page<Task> findByProjectIdAndAssignedTo(Long projectId, java.util.UUID assignedTo, Pageable pageable);

    // --- Search by title only ---
    Page<Task> findByProjectIdAndTitleContainingIgnoreCase(Long projectId, String title, Pageable pageable);

    // --- status + priority ---
    Page<Task> findByProjectIdAndStatusAndPriority(
            Long projectId, TaskStatus status, TaskPriority priority, Pageable pageable);

    // --- status + search ---
    Page<Task> findByProjectIdAndStatusAndTitleContainingIgnoreCase(
            Long projectId, TaskStatus status, String title, Pageable pageable);

    // --- priority + search ---
    Page<Task> findByProjectIdAndPriorityAndTitleContainingIgnoreCase(
            Long projectId, TaskPriority priority, String title, Pageable pageable);

    // --- status + priority + search ---
    Page<Task> findByProjectIdAndStatusAndPriorityAndTitleContainingIgnoreCase(
            Long projectId, TaskStatus status, TaskPriority priority, String title, Pageable pageable);

    // --- assignee + status ---
    Page<Task> findByProjectIdAndAssignedToAndStatus(
            Long projectId, java.util.UUID assignedTo, TaskStatus status, Pageable pageable);

    // Delete all tasks for a project (cascade on project delete)
    void deleteByProjectId(Long projectId);
}
