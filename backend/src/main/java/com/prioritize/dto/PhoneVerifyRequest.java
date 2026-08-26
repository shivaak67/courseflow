package com.prioritize.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneVerifyRequest(
        @NotBlank(message = "code is required")
        String code) {
}
