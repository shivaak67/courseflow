package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

public record StudySessionResponse(
        UUID id,
        UUID assignmentId,
        String assignmentTitle,
        String courseName,
        Instant startedAt,
        Instant endedAt,
        Integer durationMinutes,
        String notes,
        Instant createdAt) {
}
