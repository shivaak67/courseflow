package com.prioritize.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prioritize.model.ScheduleBlock;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, UUID> {

    Optional<ScheduleBlock> findByIdAndUserId(UUID id, UUID userId);

    List<ScheduleBlock> findByUserIdOrderByStartAtAsc(UUID userId);

    List<ScheduleBlock> findByUserIdAndTaskIdOrderByStartAtAsc(UUID userId, UUID taskId);

    @Query("""
            SELECT s FROM ScheduleBlock s
            WHERE s.userId = :userId
              AND s.startAt < :to
              AND s.endAt > :from
            ORDER BY s.startAt ASC
            """)
    List<ScheduleBlock> findOverlapping(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
