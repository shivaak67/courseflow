package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

public record AppNotificationResponse(
        UUID id,
        String title,
        String body,
        String relatedEntityType,
        UUID relatedEntityId,
        Instant readAt,
        Instant createdAt) {
}
