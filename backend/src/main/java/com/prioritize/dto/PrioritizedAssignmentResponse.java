package com.prioritize.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.prioritize.model.Difficulty;
import com.prioritize.model.PriorityLevel;

public record PrioritizedAssignmentResponse(
        UUID id,
        UUID courseId,
        String courseName,
        String canvasAssignmentId,
        String title,
        String description,
        Instant dueDate,
        Double pointsPossible,
        boolean completed,
        boolean submitted,
        Difficulty difficulty,
        Double estimatedHours,
        Double actualHours,
        Integer personalPriority,
        Double priorityScore,
        PriorityLevel priorityLevel,
        Instant createdAt,
        Instant updatedAt,
        List<String> reasons) {
}
