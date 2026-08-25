package com.prioritize.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prioritize.model.Reminder;
import com.prioritize.model.ReminderStatus;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    Optional<Reminder> findByIdAndUserId(UUID id, UUID userId);

    List<Reminder> findByUserIdOrderByReminderAtDesc(UUID userId);

    List<Reminder> findByUserIdAndStatusOrderByReminderAtDesc(UUID userId, ReminderStatus status);

    @Query("""
            SELECT r FROM Reminder r
            WHERE r.status = com.prioritize.model.ReminderStatus.PENDING
              AND r.reminderAt <= :now
            ORDER BY r.reminderAt ASC
            """)
    List<Reminder> findDuePending(@Param("now") Instant now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Reminder r
            SET r.status = com.prioritize.model.ReminderStatus.PROCESSING,
                r.attemptCount = r.attemptCount + 1,
                r.updatedAt = :now,
                r.failureReason = NULL
            WHERE r.id = :id
              AND r.status = com.prioritize.model.ReminderStatus.PENDING
            """)
    int claimPending(@Param("id") UUID id, @Param("now") Instant now);
}
