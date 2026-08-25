package com.prioritize.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.NotificationSettingsRequest;
import com.prioritize.dto.NotificationSettingsResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.NotificationSettingsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;
    private final CurrentUserService currentUserService;

    public NotificationSettingsController(
            NotificationSettingsService notificationSettingsService,
            CurrentUserService currentUserService) {
        this.notificationSettingsService = notificationSettingsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public NotificationSettingsResponse get() {
        UUID userId = currentUserService.requireCurrentUserId();
        return notificationSettingsService.getOrCreate(userId);
    }

    @PutMapping
    public NotificationSettingsResponse update(@Valid @RequestBody NotificationSettingsRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return notificationSettingsService.update(userId, request);
    }
}
