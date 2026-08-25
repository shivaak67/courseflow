package com.prioritize.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prioritize.model.Project;
import com.prioritize.model.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Project> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT p FROM Project p
            WHERE p.userId = :userId
              AND (:status IS NULL OR p.status = :status)
              AND (:goalId IS NULL OR p.goalId = :goalId)
            ORDER BY p.createdAt DESC
            """)
    List<Project> findFiltered(
            @Param("userId") UUID userId,
            @Param("status") ProjectStatus status,
            @Param("goalId") UUID goalId);
}
