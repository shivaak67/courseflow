package com.prioritize.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prioritize.model.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.course
            WHERE a.userId = :userId
              AND (:courseId IS NULL OR a.course.id = :courseId)
              AND (:completed IS NULL OR a.completed = :completed)
            ORDER BY a.dueDate ASC NULLS LAST, a.title ASC
            """)
    List<Assignment> findFiltered(
            @Param("userId") UUID userId,
            @Param("courseId") UUID courseId,
            @Param("completed") Boolean completed);

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.course
            WHERE a.id = :id AND a.userId = :userId
            """)
    Optional<Assignment> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.course
            WHERE a.userId = :userId
              AND a.completed = false
              AND a.submitted = false
              AND a.dueDate IS NOT NULL
              AND a.dueDate >= :now
            ORDER BY a.dueDate ASC
            """)
    List<Assignment> findUpcoming(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.course
            WHERE a.userId = :userId
              AND a.completed = false
              AND a.submitted = false
              AND a.dueDate IS NOT NULL
              AND a.dueDate < :now
            ORDER BY a.dueDate ASC
            """)
    List<Assignment> findOverdue(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.course
            WHERE a.userId = :userId
              AND a.completed = false
              AND a.submitted = false
            ORDER BY a.dueDate ASC NULLS LAST, a.title ASC
            """)
    List<Assignment> findOpenOrderedByDueDate(@Param("userId") UUID userId);

    @Query("""
            SELECT a FROM Assignment a
            JOIN FETCH a.course
            WHERE a.userId = :userId
              AND a.canvasAssignmentId = :canvasAssignmentId
            """)
    Optional<Assignment> findByUserIdAndCanvasAssignmentId(
            @Param("userId") UUID userId,
            @Param("canvasAssignmentId") String canvasAssignmentId);
}
