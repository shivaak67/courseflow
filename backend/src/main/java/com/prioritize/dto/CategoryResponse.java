package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String icon,
        String color,
        Instant createdAt,
        Instant updatedAt) {
}
