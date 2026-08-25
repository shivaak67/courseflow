package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.ReminderRequest;
import com.prioritize.dto.ReminderResponse;
import com.prioritize.dto.ReminderUpdateRequest;
import com.prioritize.model.NotificationChannel;
import com.prioritize.model.Reminder;
import com.prioritize.model.ReminderStatus;

@Component
public class ReminderMapper {

    public ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getRelatedEntityType(),
                reminder.getRelatedEntityId(),
                reminder.getReminderAt(),
                reminder.getChannel(),
                reminder.getStatus(),
                reminder.getSentAt(),
                reminder.getAttemptCount(),
                reminder.getFailureReason(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt());
    }

    public void applyCreate(Reminder reminder, ReminderRequest request) {
        reminder.setRelatedEntityType(request.relatedEntityType());
        reminder.setRelatedEntityId(request.relatedEntityId());
        reminder.setReminderAt(request.reminderAt());
        reminder.setChannel(request.channel() != null ? request.channel() : NotificationChannel.IN_APP);
        reminder.setStatus(ReminderStatus.PENDING);
        reminder.setAttemptCount(0);
        reminder.setSentAt(null);
        reminder.setFailureReason(null);
    }

    public void applyUpdate(Reminder reminder, ReminderUpdateRequest request) {
        if (request.reminderAt() != null) {
            reminder.setReminderAt(request.reminderAt());
        }
        if (request.channel() != null) {
            reminder.setChannel(request.channel());
        }
    }
}
