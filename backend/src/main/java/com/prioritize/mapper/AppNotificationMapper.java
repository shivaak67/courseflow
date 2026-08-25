package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.AppNotificationResponse;
import com.prioritize.model.AppNotification;

@Component
public class AppNotificationMapper {

    public AppNotificationResponse toResponse(AppNotification notification) {
        return new AppNotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
