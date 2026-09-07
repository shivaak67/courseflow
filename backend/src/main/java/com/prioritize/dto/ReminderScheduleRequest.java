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
        @NotEmpty List<@NotNull @jakarta.validation.constraints.Positive Integer> offsetMinutes,
        @NotEmpty List<@NotNull NotificationChannel> channels,
        @jakarta.validation.constraints.Size(max = 64) String timeZone) {
}
