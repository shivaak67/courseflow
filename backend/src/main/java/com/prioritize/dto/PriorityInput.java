package com.prioritize.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Plain scoring input — intentionally independent of JPA Assignment entities
 * so the priority engine can land in parallel with domain CRUD.
 *
 * @param dueDate            assignment due instant, or null if unknown
 * @param pointsPossible     points the assignment is worth, or null
 * @param courseTotalPoints  optional course total for relative point scoring
 * @param difficulty         {@code EASY}, {@code MEDIUM}, {@code HARD}, or null
 * @param estimatedHours     estimated effort in hours, or null
 * @param personalPriority   student priority 1–5, or null
 * @param completed          whether the student marked it complete
 * @param submitted          whether it was submitted (e.g. via Canvas)
 */
public record PriorityInput(
        Instant dueDate,
        BigDecimal pointsPossible,
        BigDecimal courseTotalPoints,
        String difficulty,
        BigDecimal estimatedHours,
        Integer personalPriority,
        boolean completed,
        boolean submitted
) {
}
