package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.TimeEntryRequest;
import com.prioritize.dto.TimeEntryResponse;
import com.prioritize.model.TimeEntry;

@Component
public class TimeEntryMapper {

    public TimeEntryResponse toResponse(TimeEntry entry) {
        return new TimeEntryResponse(
                entry.getId(),
                entry.getTaskId(),
                entry.getStartedAt(),
                entry.getEndedAt(),
                entry.getDurationMinutes(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }

    public void applyCreate(TimeEntry entry, TimeEntryRequest request, int durationMinutes) {
        entry.setTaskId(request.taskId());
        entry.setStartedAt(request.startedAt());
        entry.setEndedAt(request.endedAt());
        entry.setDurationMinutes(durationMinutes);
        entry.setNotes(request.notes());
    }

    public void applyUpdate(TimeEntry entry, TimeEntryRequest request, int durationMinutes) {
        entry.setTaskId(request.taskId());
        entry.setStartedAt(request.startedAt());
        entry.setEndedAt(request.endedAt());
        entry.setDurationMinutes(durationMinutes);
        entry.setNotes(request.notes());
    }
}
