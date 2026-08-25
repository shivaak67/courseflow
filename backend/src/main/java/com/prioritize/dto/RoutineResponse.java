package com.prioritize.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.prioritize.model.RecurrenceType;

public record RoutineResponse(
        UUID id,
        UUID categoryId,
        String title,
        RecurrenceType recurrenceType,
        String daysOfWeek,
        int intervalValue,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
