package com.prioritize.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prioritize.model.AppNotification;

public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

    List<AppNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AppNotification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<AppNotification> findByIdAndUserId(UUID id, UUID userId);
}
