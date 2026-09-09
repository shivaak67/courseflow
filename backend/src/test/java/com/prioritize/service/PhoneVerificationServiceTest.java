package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.prioritize.dto.PhoneVerifyRequest;
import com.prioritize.exception.ApiException;
import com.prioritize.model.User;
import com.prioritize.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {
    @Mock private UserRepository users;
    @Mock private SmsNotificationService sms;
    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-05T12:00:00Z");
    private User user;
    private PhoneVerificationService service;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setPhoneNumber("+15551234567");
        when(users.findById(userId)).thenReturn(Optional.of(user));
        service = new PhoneVerificationService(users, sms, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void failedTextReturnsServiceUnavailableInsteadOfFalseSuccess() {
        doThrow(new NotificationDeliveryException("SMS delivery is not configured"))
                .when(sms).send(anyString(), anyString());
        assertThatThrownBy(() -> service.sendCode(userId))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void sentCodeCanVerifyPhoneAndIsClearedAfterUse() {
        service.sendCode(userId);
        String code = user.getPhoneVerificationCode();
        assertThat(code).matches("\\d{6}");
        assertThat(user.getPhoneVerificationExpiresAt()).isEqualTo(now.plusSeconds(900));
        verify(sms).send(eq(user.getPhoneNumber()), eq("Your Prioritize verification code is " + code));
        service.verify(userId, new PhoneVerifyRequest(code));
        assertThat(user.isPhoneVerified()).isTrue();
        assertThat(user.getPhoneVerificationCode()).isNull();
        assertThat(user.getPhoneVerificationExpiresAt()).isNull();
    }
}
