package com.prioritize.dto;

import java.time.Instant;

import com.prioritize.model.Difficulty;

import jakarta.validation.constraints.Size;

public record AssignmentUpdateRequest(
        @Size(max = 500) String title,
        String description,
        Instant dueDate,
        Double pointsPossible,
        Boolean completed,
        Difficulty difficulty,
        Double estimatedHours,
        Double actualHours,
        Integer personalPriority) {
}
