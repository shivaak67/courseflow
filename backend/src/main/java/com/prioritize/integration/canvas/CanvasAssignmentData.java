package com.prioritize.integration.canvas;

import java.time.Instant;

public record CanvasAssignmentData(
        String canvasAssignmentId,
        String title,
        String description,
        Instant dueAt,
        Double pointsPossible,
        boolean submitted,
        boolean completed) {
}
