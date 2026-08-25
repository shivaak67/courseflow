package com.prioritize.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.prioritize.model.TaskPriority;
import com.prioritize.model.TaskStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        UUID categoryId,
        UUID projectId,
        LocalDate dueDate,
        LocalTime dueTime,
        @Min(0) Integer estimatedMinutes,
        TaskPriority priority,
        TaskStatus status) {
}
