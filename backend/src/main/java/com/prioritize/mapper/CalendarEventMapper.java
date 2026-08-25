package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.CalendarEventRequest;
import com.prioritize.dto.CalendarEventResponse;
import com.prioritize.model.CalendarEvent;

@Component
public class CalendarEventMapper {

    public CalendarEventResponse toResponse(CalendarEvent event) {
        return new CalendarEventResponse(
                event.getId(),
                event.getCategoryId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.isAllDay(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    public void applyCreate(CalendarEvent event, CalendarEventRequest request) {
        applyFields(event, request);
        event.setAllDay(request.allDay() != null && request.allDay());
    }

    public void applyUpdate(CalendarEvent event, CalendarEventRequest request) {
        applyFields(event, request);
        if (request.allDay() != null) {
            event.setAllDay(request.allDay());
        }
    }

    private void applyFields(CalendarEvent event, CalendarEventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(blankToNull(request.description()));
        event.setCategoryId(request.categoryId());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
