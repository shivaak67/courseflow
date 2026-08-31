package com.prioritize.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.ReminderRequest;
import com.prioritize.dto.ReminderResponse;
import com.prioritize.dto.ReminderUpdateRequest;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.ReminderMapper;
import com.prioritize.model.NotificationChannel;
import com.prioritize.model.Reminder;
import com.prioritize.model.ReminderEntityType;
import com.prioritize.model.ReminderStatus;
import com.prioritize.repository.CalendarEventRepository;
import com.prioritize.repository.GoalRepository;
import com.prioritize.repository.ReminderRepository;
import com.prioritize.repository.RoutineRepository;
import com.prioritize.repository.ScheduleBlockRepository;
import com.prioritize.repository.TaskRepository;

@Service
@Transactional
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final TaskRepository taskRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final RoutineRepository routineRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final GoalRepository goalRepository;
    private final ReminderMapper reminderMapper;

    public ReminderService(
            ReminderRepository reminderRepository,
            TaskRepository taskRepository,
            ScheduleBlockRepository scheduleBlockRepository,
            RoutineRepository routineRepository,
            CalendarEventRepository calendarEventRepository,
            GoalRepository goalRepository,
            ReminderMapper reminderMapper) {
        this.reminderRepository = reminderRepository;
        this.taskRepository = taskRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.routineRepository = routineRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.goalRepository = goalRepository;
        this.reminderMapper = reminderMapper;
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> list(UUID userId, ReminderStatus status) {
        List<Reminder> reminders = status == null
                ? reminderRepository.findByUserIdOrderByReminderAtDesc(userId)
                : reminderRepository.findByUserIdAndStatusOrderByReminderAtDesc(userId, status);
        return reminders.stream().map(reminderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReminderResponse get(UUID userId, UUID reminderId) {
        return reminderMapper.toResponse(requireOwned(userId, reminderId));
    }

    public ReminderResponse create(UUID userId, ReminderRequest request) {
        validateChannelSupported(request.channel());
        validateRelatedEntityOwned(userId, request.relatedEntityType(), request.relatedEntityId());
        Reminder reminder = new Reminder();
        reminder.setUserId(userId);
        reminderMapper.applyCreate(reminder, request);
        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    public ReminderResponse update(UUID userId, UUID reminderId, ReminderUpdateRequest request) {
        Reminder reminder = requireOwned(userId, reminderId);
        if (reminder.getStatus() != ReminderStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING reminders can update time or channel");
        }
        if (request.reminderAt() == null && request.channel() == null) {
            throw new IllegalArgumentException("At least one of reminderAt or channel is required");
        }
        validateChannelSupported(request.channel());
        reminderMapper.applyUpdate(reminder, request);
        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    public ReminderResponse cancel(UUID userId, UUID reminderId) {
        Reminder reminder = requireOwned(userId, reminderId);
        if (reminder.getStatus() != ReminderStatus.CANCELLED) {
            reminder.setStatus(ReminderStatus.CANCELLED);
            reminder.setFailureReason(null);
            reminderRepository.save(reminder);
        }
        return reminderMapper.toResponse(reminder);
    }

    public void delete(UUID userId, UUID reminderId) {
        Reminder reminder = requireOwned(userId, reminderId);
        reminderRepository.delete(reminder);
    }

    @Transactional(readOnly = true)
    public Reminder requireOwned(UUID userId, UUID reminderId) {
        return reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found"));
    }

    void validateRelatedEntityOwned(UUID userId, ReminderEntityType type, UUID entityId) {
        boolean owned = switch (type) {
            case TASK -> taskRepository.findByIdAndUserId(entityId, userId).isPresent();
            case SCHEDULE_BLOCK -> scheduleBlockRepository.findByIdAndUserId(entityId, userId).isPresent();
            case ROUTINE -> routineRepository.findByIdAndUserId(entityId, userId).isPresent();
            case CALENDAR_EVENT -> calendarEventRepository.findByIdAndUserId(entityId, userId).isPresent();
            case GOAL -> goalRepository.findByIdAndUserId(entityId, userId).isPresent();
        };
        if (!owned) {
            throw new ResourceNotFoundException(type.name() + " not found");
        }
    }

    private static void validateChannelSupported(NotificationChannel channel) {
        NotificationChannel resolved = channel != null ? channel : NotificationChannel.EMAIL;
        if (resolved != NotificationChannel.EMAIL && resolved != NotificationChannel.SMS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only EMAIL and SMS reminders are supported");
        }
    }

    static void validateChannelSupportedOrThrow(NotificationChannel channel) {
        validateChannelSupported(channel);
    }
}
