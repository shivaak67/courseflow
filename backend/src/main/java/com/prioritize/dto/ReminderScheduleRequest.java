package com.prioritize.dto;

import java.util.List;
import java.util.UUID;

import com.prioritize.model.NotificationChannel;
import com.prioritize.model.ReminderEntityType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ReminderScheduleRequest(
        @NotNull ReminderEntityType relatedEntityType,
        @NotNull UUID relatedEntityId,
        @NotEmpty List<Integer> offsetMinutes,
        @NotEmpty List<NotificationChannel> channels) {
}
