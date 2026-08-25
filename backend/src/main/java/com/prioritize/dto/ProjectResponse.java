package com.prioritize.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.prioritize.model.ProjectStatus;

public record ProjectResponse(
        UUID id,
        UUID categoryId,
        UUID goalId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate targetDate,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
