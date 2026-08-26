package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimeEntryRequest(
        @NotNull UUID taskId,
        Instant startedAt,
        Instant endedAt,
        @Min(0) Integer durationMinutes,
        String notes) {
}
