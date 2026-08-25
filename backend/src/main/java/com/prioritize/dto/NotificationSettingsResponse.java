package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationSettingsResponse(
        UUID userId,
        boolean smsEnabled,
        boolean inAppEnabled,
        boolean emailEnabled,
        String defaultReminderOffsetsMinutes,
        Instant createdAt,
        Instant updatedAt) {
}
