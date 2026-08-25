package com.prioritize.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prioritize.model.Task;
import com.prioritize.model.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND (:projectId IS NULL OR t.projectId = :projectId)
              AND (:status IS NULL OR t.status = :status)
              AND (:categoryId IS NULL OR t.categoryId = :categoryId)
            ORDER BY t.createdAt DESC
            """)
    List<Task> findFiltered(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId,
            @Param("status") TaskStatus status,
            @Param("categoryId") UUID categoryId);
}
