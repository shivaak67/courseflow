package com.prioritize.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.PhoneStatusResponse;
import com.prioritize.dto.PhoneUpdateRequest;
import com.prioritize.dto.PhoneVerifyRequest;
import com.prioritize.exception.ApiException;
import com.prioritize.model.User;
import com.prioritize.repository.UserRepository;

@Service
@Transactional
public class PhoneVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final SmsNotificationService smsNotificationService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneVerificationService(
            UserRepository userRepository,
            SmsNotificationService smsNotificationService,
            Clock clock) {
        this.userRepository = userRepository;
        this.smsNotificationService = smsNotificationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PhoneStatusResponse status(UUID userId) {
        User user = requireUser(userId);
        return toStatus(user);
    }

    public PhoneStatusResponse updatePhone(UUID userId, PhoneUpdateRequest request) {
        User user = requireUser(userId);
        String normalized = normalizePhone(request.phoneNumber());
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid phone number in E.164 format (e.g. +15551234567)");
        }

        user.setPhoneNumber(normalized);
        user.setPhoneVerified(false);
        user.setPhoneVerificationCode(null);
        user.setPhoneVerificationExpiresAt(null);
        userRepository.save(user);

        return sendVerificationCode(user);
    }

    public PhoneStatusResponse sendCode(UUID userId) {
        User user = requireUser(userId);
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Add a phone number first");
        }
        return sendVerificationCode(user);
    }

    public PhoneStatusResponse verify(UUID userId, PhoneVerifyRequest request) {
        User user = requireUser(userId);
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Add a phone number first");
        }
        if (user.getPhoneVerificationCode() == null || user.getPhoneVerificationExpiresAt() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request a verification code first");
        }
        if (Instant.now(clock).isAfter(user.getPhoneVerificationExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification code expired — request a new one");
        }
        if (!user.getPhoneVerificationCode().equals(request.code().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid verification code");
        }

        user.setPhoneVerified(true);
        user.setPhoneVerificationCode(null);
        user.setPhoneVerificationExpiresAt(null);
        userRepository.save(user);
        return toStatus(user);
    }

    private PhoneStatusResponse sendVerificationCode(User user) {
        String code = generateCode();
        user.setPhoneVerificationCode(code);
        user.setPhoneVerificationExpiresAt(Instant.now(clock).plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        userRepository.save(user);

        String message = "Your Prioritize verification code is " + code;
        try {
            smsNotificationService.send(user.getPhoneNumber(), message);
        } catch (NotificationDeliveryException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not send a verification text. Please try again later.");
        }

        return toStatus(user);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static PhoneStatusResponse toStatus(User user) {
        return new PhoneStatusResponse(user.getPhoneNumber(), user.isPhoneVerified());
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int value = secureRandom.nextInt(bound);
        return String.format("%0" + CODE_LENGTH + "d", value);
    }

    static String normalizePhone(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String digits = trimmed.replaceAll("[^0-9+]", "");
        if (!digits.startsWith("+")) {
            digits = "+" + digits.replace("+", "");
        }
        if (!digits.matches("^\\+[1-9]\\d{7,14}$")) {
            return null;
        }
        return digits;
    }
}
