package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.OccurrenceResponse;
import com.prioritize.dto.RoutineRequest;
import com.prioritize.dto.RoutineResponse;
import com.prioritize.model.RecurrenceType;
import com.prioritize.model.Routine;

@Component
public class RoutineMapper {

    public RoutineResponse toResponse(Routine routine) {
        return new RoutineResponse(
                routine.getId(),
                routine.getCategoryId(),
                routine.getTitle(),
                routine.getRecurrenceType(),
                routine.getDaysOfWeek(),
                routine.getIntervalValue(),
                routine.getStartTime(),
                routine.getEndTime(),
                routine.getStartDate(),
                routine.getEndDate(),
                routine.isActive(),
                routine.getCreatedAt(),
                routine.getUpdatedAt());
    }

    public OccurrenceResponse toOccurrence(Routine routine, java.time.LocalDate date) {
        return new OccurrenceResponse(
                routine.getId(),
                routine.getTitle(),
                date,
                routine.getStartTime(),
                routine.getEndTime(),
                routine.getRecurrenceType());
    }

    public void applyCreate(Routine routine, RoutineRequest request) {
        applyFields(routine, request);
        routine.setActive(request.active() == null || request.active());
    }

    public void applyUpdate(Routine routine, RoutineRequest request) {
        applyFields(routine, request);
        if (request.active() != null) {
            routine.setActive(request.active());
        }
    }

    private void applyFields(Routine routine, RoutineRequest request) {
        routine.setTitle(request.title().trim());
        routine.setCategoryId(request.categoryId());
        routine.setRecurrenceType(request.recurrenceType());
        routine.setDaysOfWeek(normalizeDaysOfWeek(request.recurrenceType(), request.daysOfWeek()));
        routine.setIntervalValue(request.intervalValue() != null ? request.intervalValue() : 1);
        routine.setStartTime(request.startTime());
        routine.setEndTime(request.endTime());
        routine.setStartDate(request.startDate());
        routine.setEndDate(request.endDate());
    }

    private String normalizeDaysOfWeek(RecurrenceType type, String daysOfWeek) {
        if (type == RecurrenceType.DAILY || type == RecurrenceType.MONTHLY) {
            return null;
        }
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return null;
        }
        return daysOfWeek.trim();
    }
}
