package com.prioritize.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.prioritize.model.GoalStatus;

public record GoalResponse(
        UUID id,
        UUID categoryId,
        String title,
        String description,
        LocalDate targetDate,
        GoalStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
