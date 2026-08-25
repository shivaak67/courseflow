package com.prioritize.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.prioritize.model.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        UUID categoryId,
        UUID goalId,
        LocalDate startDate,
        LocalDate targetDate,
        ProjectStatus status) {
}
