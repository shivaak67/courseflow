package com.prioritize.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.prioritize.model.RecurrenceType;

public record OccurrenceResponse(
        UUID routineId,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        RecurrenceType recurrenceType) {
}
