package com.prioritize.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.prioritize.model.RecurrenceType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoutineRequest(
        @NotBlank @Size(max = 255) String title,
        UUID categoryId,
        @NotNull RecurrenceType recurrenceType,
        @Size(max = 32) String daysOfWeek,
        @Min(1) Integer intervalValue,
        @NotNull LocalTime startTime,
        LocalTime endTime,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        Boolean active) {
}
