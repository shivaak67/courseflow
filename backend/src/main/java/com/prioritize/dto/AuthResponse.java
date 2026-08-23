package com.prioritize.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn,
        UserResponse user) {
}
