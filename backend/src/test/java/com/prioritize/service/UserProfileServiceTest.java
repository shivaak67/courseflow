package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.prioritize.dto.UserProfileRequest;
import com.prioritize.dto.UserProfileResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.model.AuthProvider;
import com.prioritize.model.Role;
import com.prioritize.model.User;
import com.prioritize.repository.UserRepository;
import com.prioritize.sms.LoggingSmsSender;
import com.prioritize.sms.SmsSender;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");

    @Mock
    private UserRepository userRepository;

    private SmsSender smsSender;
    private UserProfileService userProfileService;
    private User user;

    @BeforeEach
    void setUp() {
        smsSender = new LoggingSmsSender();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        userProfileService = new UserProfileService(userRepository, smsSender, clock);

        user = new User();
        user.setId(USER_ID);
        user.setEmail("ada@example.com");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(Role.USER);
        user.setTimezone("UTC");
        user.setPhoneNumber("+15551234567");
        user.setPhoneVerified(true);
    }

    @Test
    void updatePhoneNumberClearsVerified() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileService.updateProfile(
                USER_ID,
                new UserProfileRequest(null, "+15559876543"));

        assertThat(response.phoneNumber()).isEqualTo("+15559876543");
        assertThat(response.phoneVerified()).isFalse();
        assertThat(user.isPhoneVerified()).isFalse();
        assertThat(user.getPhoneVerificationCode()).isNull();
    }

    @Test
    void requestAndVerifyPhoneSucceedsWithLoggingSmsSender() {
        user.setPhoneVerified(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userProfileService.requestPhoneVerification(USER_ID);

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        String code = savedCaptor.getValue().getPhoneVerificationCode();
        assertThat(code).matches("\\d{6}");
        assertThat(savedCaptor.getValue().getPhoneVerificationExpiresAt()).isEqualTo(NOW.plusSeconds(600));

        userProfileService.verifyPhone(USER_ID, code);

        assertThat(user.isPhoneVerified()).isTrue();
        assertThat(user.getPhoneVerificationCode()).isNull();
        assertThat(user.getPhoneVerificationExpiresAt()).isNull();
    }

    @Test
    void verifyPhoneRejectsBadCode() {
        user.setPhoneVerified(false);
        user.setPhoneVerificationCode("123456");
        user.setPhoneVerificationExpiresAt(NOW.plusSeconds(600));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userProfileService.verifyPhone(USER_ID, "000000"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void verifyPhoneRejectsExpiredCode() {
        user.setPhoneVerified(false);
        user.setPhoneVerificationCode("123456");
        user.setPhoneVerificationExpiresAt(NOW.minusSeconds(1));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userProfileService.verifyPhone(USER_ID, "123456"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void requestVerificationRequiresPhone() {
        user.setPhoneNumber(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userProfileService.requestPhoneVerification(USER_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("phone number missing");
    }
}
