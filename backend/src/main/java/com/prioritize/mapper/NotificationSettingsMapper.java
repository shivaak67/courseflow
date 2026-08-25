package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.NotificationSettingsRequest;
import com.prioritize.dto.NotificationSettingsResponse;
import com.prioritize.model.NotificationSettings;

@Component
public class NotificationSettingsMapper {

    public NotificationSettingsResponse toResponse(NotificationSettings settings) {
        return new NotificationSettingsResponse(
                settings.getUserId(),
                settings.isSmsEnabled(),
                settings.isInAppEnabled(),
                settings.isEmailEnabled(),
                settings.getDefaultReminderOffsetsMinutes(),
                settings.getCreatedAt(),
                settings.getUpdatedAt());
    }

    public void applyUpdate(NotificationSettings settings, NotificationSettingsRequest request) {
        if (request.smsEnabled() != null) {
            settings.setSmsEnabled(request.smsEnabled());
        }
        if (request.inAppEnabled() != null) {
            settings.setInAppEnabled(request.inAppEnabled());
        }
        if (request.emailEnabled() != null) {
            settings.setEmailEnabled(request.emailEnabled());
        }
        if (request.defaultReminderOffsetsMinutes() != null) {
            String value = request.defaultReminderOffsetsMinutes().trim();
            settings.setDefaultReminderOffsetsMinutes(value.isEmpty() ? null : value);
        }
    }
}
