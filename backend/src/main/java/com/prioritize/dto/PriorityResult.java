package com.prioritize.dto;

import java.util.List;

/**
 * Transparent priority outcome: numeric score, band, and human-readable factors.
 *
 * @param score  weighted score on a 0–100 scale (0 when completed/submitted)
 * @param level  CRITICAL / HIGH / MEDIUM / LOW
 * @param reasons short explanations of the dominant scoring factors
 */
public record PriorityResult(
        double score,
        PriorityLevel level,
        List<String> reasons
) {
}
