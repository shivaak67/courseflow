package com.prioritize.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.NotificationSettingsRequest;
import com.prioritize.dto.NotificationSettingsResponse;
import com.prioritize.mapper.NotificationSettingsMapper;
import com.prioritize.model.NotificationSettings;
import com.prioritize.repository.NotificationSettingsRepository;

@Service
@Transactional
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;
    private final NotificationSettingsMapper notificationSettingsMapper;

    public NotificationSettingsService(
            NotificationSettingsRepository notificationSettingsRepository,
            NotificationSettingsMapper notificationSettingsMapper) {
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.notificationSettingsMapper = notificationSettingsMapper;
    }

    public NotificationSettingsResponse getOrCreate(UUID userId) {
        return notificationSettingsMapper.toResponse(requireOrCreate(userId));
    }

    public NotificationSettingsResponse update(UUID userId, NotificationSettingsRequest request) {
        NotificationSettings settings = requireOrCreate(userId);
        notificationSettingsMapper.applyUpdate(settings, request);
        return notificationSettingsMapper.toResponse(notificationSettingsRepository.save(settings));
    }

    public NotificationSettings requireOrCreate(UUID userId) {
        return notificationSettingsRepository.findById(userId).orElseGet(() -> {
            NotificationSettings defaults = new NotificationSettings();
            defaults.setUserId(userId);
            defaults.setInAppEnabled(true);
            defaults.setSmsEnabled(false);
            defaults.setEmailEnabled(false);
            return notificationSettingsRepository.save(defaults);
        });
    }
}
