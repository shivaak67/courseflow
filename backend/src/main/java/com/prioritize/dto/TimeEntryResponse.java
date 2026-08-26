package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

public record TimeEntryResponse(
        UUID id,
        UUID taskId,
        Instant startedAt,
        Instant endedAt,
        int durationMinutes,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
