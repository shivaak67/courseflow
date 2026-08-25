package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

import com.prioritize.model.NotificationChannel;
import com.prioritize.model.ReminderEntityType;
import com.prioritize.model.ReminderStatus;

public record ReminderResponse(
        UUID id,
        ReminderEntityType relatedEntityType,
        UUID relatedEntityId,
        Instant reminderAt,
        NotificationChannel channel,
        ReminderStatus status,
        Instant sentAt,
        int attemptCount,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {
}
