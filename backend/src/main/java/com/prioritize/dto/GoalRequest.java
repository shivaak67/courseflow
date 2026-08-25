package com.prioritize.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.prioritize.model.GoalStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoalRequest(
        UUID categoryId,
        @NotBlank @Size(max = 255) String title,
        String description,
        LocalDate targetDate,
        GoalStatus status) {
}
