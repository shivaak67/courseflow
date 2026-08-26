package com.prioritize.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String timezone,
        String phoneNumber,
        boolean phoneVerified) {
}
