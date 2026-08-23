package com.prioritize.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.prioritize.security.JwtService;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-jwt-secret-key-at-least-32-chars-long",
            86400000L);

    @Test
    void generateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "user@example.com");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.getExpirationMs()).isEqualTo(86400000L);
    }

    @Test
    void rejectsMalformedToken() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }
}
