package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

public record ScheduleBlockResponse(
        UUID id,
        UUID taskId,
        Instant startAt,
        Instant endAt,
        boolean completed,
        Instant createdAt,
        Instant updatedAt) {
}
