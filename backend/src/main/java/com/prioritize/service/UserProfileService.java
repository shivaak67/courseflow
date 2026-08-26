package com.prioritize.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.UserProfileRequest;
import com.prioritize.dto.UserProfileResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.model.User;
import com.prioritize.repository.UserRepository;
import com.prioritize.sms.SmsSender;

@Service
@Transactional
public class UserProfileService {

    private static final int VERIFICATION_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final SmsSender smsSender;
    private final Clock clock;

    public UserProfileService(UserRepository userRepository, SmsSender smsSender, Clock clock) {
        this.userRepository = userRepository;
        this.smsSender = smsSender;
        this.clock = clock;
    }

    public UserProfileResponse getProfile(UUID userId) {
        return toResponse(requireUser(userId));
    }

    public UserProfileResponse updateProfile(UUID userId, UserProfileRequest request) {
        User user = requireUser(userId);

        if (request.timezone() != null) {
            String timezone = request.timezone().isBlank() ? "UTC" : request.timezone().trim();
            user.setTimezone(timezone);
        }

        if (request.phoneNumber() != null) {
            String phone = request.phoneNumber().isBlank() ? null : request.phoneNumber().trim();
            if (!Objects.equals(phone, user.getPhoneNumber())) {
                user.setPhoneNumber(phone);
                user.setPhoneVerified(false);
                user.setPhoneVerificationCode(null);
                user.setPhoneVerificationExpiresAt(null);
            }
        }

        return toResponse(userRepository.save(user));
    }

    public void requestPhoneVerification(UUID userId) {
        User user = requireUser(userId);
        String phone = user.getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "phone number missing");
        }
        if (!smsSender.isConfigured()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SMS not configured");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now(clock).plus(VERIFICATION_TTL_MINUTES, ChronoUnit.MINUTES);
        user.setPhoneVerificationCode(code);
        user.setPhoneVerificationExpiresAt(expiresAt);
        userRepository.save(user);

        smsSender.send(phone, "Your Prioritize code is " + code);
    }

    public void verifyPhone(UUID userId, String code) {
        User user = requireUser(userId);
        String expected = user.getPhoneVerificationCode();
        Instant expiresAt = user.getPhoneVerificationExpiresAt();
        Instant now = Instant.now(clock);

        if (expected == null
                || expiresAt == null
                || now.isAfter(expiresAt)
                || code == null
                || !expected.equals(code.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
        }

        user.setPhoneVerified(true);
        user.setPhoneVerificationCode(null);
        user.setPhoneVerificationExpiresAt(null);
        userRepository.save(user);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getTimezone(),
                user.getPhoneNumber(),
                user.isPhoneVerified());
    }
}
