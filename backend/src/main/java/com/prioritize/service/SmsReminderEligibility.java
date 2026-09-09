package com.prioritize.service;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.prioritize.config.NotificationProperties;
import com.prioritize.exception.ApiException;
import com.prioritize.model.NotificationChannel;
import com.prioritize.repository.NotificationSettingsRepository;
import com.prioritize.repository.UserRepository;

@Service
public class SmsReminderEligibility {
    private final UserRepository users;
    private final NotificationSettingsRepository settings;
    private final NotificationProperties properties;

    public SmsReminderEligibility(UserRepository users, NotificationSettingsRepository settings,
            NotificationProperties properties) {
        this.users = users;
        this.settings = settings;
        this.properties = properties;
    }

    public void validate(UUID userId, NotificationChannel channel) {
        if (channel != NotificationChannel.SMS) return;
        if (!properties.getSms().isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Text reminders are temporarily unavailable.");
        }
        if (!settings.findById(userId).map(s -> s.isSmsEnabled()).orElse(false)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enable SMS reminders in Settings first.");
        }
        var user = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isPhoneVerified() || user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verify your mobile number in Settings before scheduling texts.");
        }
    }
}
