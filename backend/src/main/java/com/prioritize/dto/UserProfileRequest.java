package com.prioritize.dto;

import jakarta.validation.constraints.Pattern;

public record UserProfileRequest(
        String timezone,
        @Pattern(
                regexp = "^$|^\\+[1-9]\\d{7,14}$",
                message = "phoneNumber must be E.164 (+ and 8–15 digits) or empty to clear")
        String phoneNumber) {
}
