package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

import com.prioritize.model.NotificationChannel;
import com.prioritize.model.ReminderEntityType;

import jakarta.validation.constraints.NotNull;

public record ReminderRequest(
        @NotNull ReminderEntityType relatedEntityType,
        @NotNull UUID relatedEntityId,
        @NotNull Instant reminderAt,
        NotificationChannel channel) {
}
