package com.prioritize.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.AppNotificationResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.AppNotificationMapper;
import com.prioritize.model.AppNotification;
import com.prioritize.repository.AppNotificationRepository;

@Service
@Transactional
public class AppNotificationService {

    private final AppNotificationRepository appNotificationRepository;
    private final AppNotificationMapper appNotificationMapper;
    private final Clock clock;

    public AppNotificationService(
            AppNotificationRepository appNotificationRepository,
            AppNotificationMapper appNotificationMapper,
            Clock clock) {
        this.appNotificationRepository = appNotificationRepository;
        this.appNotificationMapper = appNotificationMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AppNotificationResponse> list(UUID userId, boolean unreadOnly) {
        List<AppNotification> notifications = unreadOnly
                ? appNotificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                : appNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(appNotificationMapper::toResponse).toList();
    }

    public AppNotificationResponse markRead(UUID userId, UUID notificationId) {
        AppNotification notification = requireOwned(userId, notificationId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now(clock));
            appNotificationRepository.save(notification);
        }
        return appNotificationMapper.toResponse(notification);
    }

    public void delete(UUID userId, UUID notificationId) {
        AppNotification notification = requireOwned(userId, notificationId);
        appNotificationRepository.delete(notification);
    }

    private AppNotification requireOwned(UUID userId, UUID notificationId) {
        return appNotificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }
}
