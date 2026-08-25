package com.prioritize.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.prioritize.model.TaskPriority;
import com.prioritize.model.TaskStatus;

public record TaskResponse(
        UUID id,
        UUID categoryId,
        UUID projectId,
        String title,
        String description,
        LocalDate dueDate,
        LocalTime dueTime,
        Integer estimatedMinutes,
        int actualMinutes,
        TaskPriority priority,
        TaskStatus status,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
}
