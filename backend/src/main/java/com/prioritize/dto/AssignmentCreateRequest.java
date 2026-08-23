package com.prioritize.dto;

import java.time.Instant;
import java.util.UUID;

import com.prioritize.model.Difficulty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignmentCreateRequest(
        @NotNull UUID courseId,
        @NotBlank @Size(max = 500) String title,
        String description,
        Instant dueDate,
        Double pointsPossible,
        Difficulty difficulty,
        Double estimatedHours,
        Integer personalPriority) {
}
