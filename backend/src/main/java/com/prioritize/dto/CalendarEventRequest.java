package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalendarEventRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        UUID categoryId,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        Boolean allDay) {
}
