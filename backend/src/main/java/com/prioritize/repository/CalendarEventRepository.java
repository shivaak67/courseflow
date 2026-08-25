package com.prioritize.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prioritize.model.CalendarEvent;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    Optional<CalendarEvent> findByIdAndUserId(UUID id, UUID userId);

    List<CalendarEvent> findByUserIdOrderByStartAtAsc(UUID userId);

    @Query("""
            SELECT e FROM CalendarEvent e
            WHERE e.userId = :userId
              AND e.startAt < :to
              AND e.endAt > :from
            ORDER BY e.startAt ASC
            """)
    List<CalendarEvent> findOverlapping(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
