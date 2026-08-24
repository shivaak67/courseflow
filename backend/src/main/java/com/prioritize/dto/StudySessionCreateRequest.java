package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StudySessionCreateRequest(
        @NotNull UUID assignmentId,
        Instant startedAt,
        Instant endedAt,
        @NotNull @Min(1) Integer durationMinutes,
        String notes) {
}
