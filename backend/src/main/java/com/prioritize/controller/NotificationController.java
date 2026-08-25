package com.prioritize.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.AppNotificationResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.AppNotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final AppNotificationService appNotificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(
            AppNotificationService appNotificationService, CurrentUserService currentUserService) {
        this.appNotificationService = appNotificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AppNotificationResponse> list(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        UUID userId = currentUserService.requireCurrentUserId();
        return appNotificationService.list(userId, unreadOnly);
    }

    @PostMapping("/{id}/read")
    public AppNotificationResponse markRead(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return appNotificationService.markRead(userId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        appNotificationService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
