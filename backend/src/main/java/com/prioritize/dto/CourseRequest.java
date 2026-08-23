package com.prioritize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String courseCode,
        @Size(max = 100) String term) {
}
