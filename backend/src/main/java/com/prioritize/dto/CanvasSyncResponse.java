package com.prioritize.dto;

import java.time.Instant;

public record CanvasSyncResponse(
        int coursesUpserted,
        int assignmentsUpserted,
        Instant lastSyncedAt) {
}
