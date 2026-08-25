package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

public record CalendarEventResponse(
        UUID id,
        UUID categoryId,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        boolean allDay,
        Instant createdAt,
        Instant updatedAt) {
}
