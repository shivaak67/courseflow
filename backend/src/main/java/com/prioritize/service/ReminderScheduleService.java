package com.prioritize.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.ReminderResponse;
import com.prioritize.dto.ReminderScheduleRequest;
import com.prioritize.dto.ReminderScheduleResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.mapper.ReminderMapper;
import com.prioritize.model.NotificationChannel;
import com.prioritize.model.Reminder;
import com.prioritize.model.ReminderStatus;
import com.prioritize.repository.ReminderRepository;

@Service
@Transactional
public class ReminderScheduleService {

    private final ReminderService reminderService;
    private final ReminderContentResolver reminderContentResolver;
    private final ReminderRepository reminderRepository;
    private final ReminderMapper reminderMapper;

    public ReminderScheduleService(
            ReminderService reminderService,
            ReminderContentResolver reminderContentResolver,
            ReminderRepository reminderRepository,
            ReminderMapper reminderMapper) {
        this.reminderService = reminderService;
        this.reminderContentResolver = reminderContentResolver;
        this.reminderRepository = reminderRepository;
        this.reminderMapper = reminderMapper;
    }

    public ReminderScheduleResponse schedule(UUID userId, ReminderScheduleRequest request) {
        reminderService.validateRelatedEntityOwned(
                userId, request.relatedEntityType(), request.relatedEntityId());

        Instant eventAt = reminderContentResolver.resolveEventAt(
                userId, request.relatedEntityType(), request.relatedEntityId());

        Set<Integer> offsets = new LinkedHashSet<>(request.offsetMinutes());
        Set<NotificationChannel> channels = new LinkedHashSet<>(request.channels());
        for (NotificationChannel channel : channels) {
            ReminderService.validateChannelSupportedOrThrow(channel);
        }

        cancelPendingForEntity(userId, request.relatedEntityType(), request.relatedEntityId());

        List<ReminderResponse> created = new ArrayList<>();
        Instant now = Instant.now();
        for (int offsetMinutes : offsets) {
            if (offsetMinutes <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Offsets must be positive minutes before the event");
            }
            Instant reminderAt = eventAt.minus(offsetMinutes, ChronoUnit.MINUTES);
            if (!reminderAt.isAfter(now)) {
                continue;
            }
            for (NotificationChannel channel : channels) {
                Reminder reminder = new Reminder();
                reminder.setUserId(userId);
                reminder.setRelatedEntityType(request.relatedEntityType());
                reminder.setRelatedEntityId(request.relatedEntityId());
                reminder.setReminderAt(reminderAt);
                reminder.setChannel(channel);
                reminder.setStatus(ReminderStatus.PENDING);
                reminder.setAttemptCount(0);
                created.add(reminderMapper.toResponse(reminderRepository.save(reminder)));
            }
        }

        return new ReminderScheduleResponse(created);
    }

    public void cancelPendingForEntity(UUID userId, com.prioritize.model.ReminderEntityType type, UUID entityId) {
        List<Reminder> pending = reminderRepository.findByUserIdAndRelatedEntityTypeAndRelatedEntityIdAndStatus(
                userId, type, entityId, ReminderStatus.PENDING);
        for (Reminder reminder : pending) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminder.setFailureReason(null);
        }
        reminderRepository.saveAll(pending);
    }
}
