package com.prioritize.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prioritize.model.NotificationSettings;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, UUID> {
}
