package com.prioritize.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prioritize.model.TimeEntry;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {

    Optional<TimeEntry> findByIdAndUserId(UUID id, UUID userId);

    List<TimeEntry> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<TimeEntry> findByUserIdAndTaskIdOrderByCreatedAtDesc(UUID userId, UUID taskId);

    List<TimeEntry> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            UUID userId, Instant from, Instant to);

    @Query("""
            SELECT COALESCE(SUM(e.durationMinutes), 0)
            FROM TimeEntry e
            WHERE e.userId = :userId AND e.taskId = :taskId
            """)
    int sumDurationByUserIdAndTaskId(@Param("userId") UUID userId, @Param("taskId") UUID taskId);

    @Query("""
            SELECT COALESCE(SUM(e.durationMinutes), 0)
            FROM TimeEntry e
            WHERE e.userId = :userId
              AND e.createdAt >= :from
              AND e.createdAt < :to
            """)
    long sumDurationByUserIdAndCreatedAtInWindow(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    /**
     * Returns [createdAt, durationMinutes] rows for entries in [from, to).
     * Callers group by UTC LocalDate; days with no entries are omitted.
     */
    @Query("""
            SELECT e.createdAt, e.durationMinutes
            FROM TimeEntry e
            WHERE e.userId = :userId
              AND e.createdAt >= :from
              AND e.createdAt < :to
            """)
    List<Object[]> findCreatedAtAndDurationInWindow(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT e.taskId, t.title, SUM(e.durationMinutes)
            FROM TimeEntry e, Task t
            WHERE e.taskId = t.id
              AND e.userId = :userId
              AND e.createdAt >= :from
              AND e.createdAt < :to
            GROUP BY e.taskId, t.title
            ORDER BY SUM(e.durationMinutes) DESC
            """)
    List<Object[]> findTopTasksByMinutesInWindow(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(c.name, 'Uncategorized'), SUM(e.durationMinutes)
            FROM TimeEntry e
            JOIN Task t ON e.taskId = t.id
            LEFT JOIN Category c ON t.categoryId = c.id
            WHERE e.userId = :userId
              AND e.createdAt >= :from
              AND e.createdAt < :to
            GROUP BY COALESCE(c.name, 'Uncategorized')
            ORDER BY SUM(e.durationMinutes) DESC
            """)
    List<Object[]> findTopCategoriesByMinutesInWindow(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to);
}
