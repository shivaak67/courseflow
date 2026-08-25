package com.prioritize.dto;

import java.time.Instant;

import com.prioritize.model.NotificationChannel;

public record ReminderUpdateRequest(
        Instant reminderAt,
        NotificationChannel channel) {
}
