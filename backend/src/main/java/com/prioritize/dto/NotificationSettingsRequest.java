package com.prioritize.dto;

public record NotificationSettingsRequest(
        Boolean smsEnabled,
        Boolean inAppEnabled,
        Boolean emailEnabled,
        String defaultReminderOffsetsMinutes) {
}
