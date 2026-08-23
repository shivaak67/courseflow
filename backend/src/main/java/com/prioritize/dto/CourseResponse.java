package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String canvasCourseId,
        String name,
        String courseCode,
        String term,
        Instant createdAt,
        Instant updatedAt) {
}
