package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ScheduleBlockRequest(
        @NotNull UUID taskId,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        Boolean completed) {
}
