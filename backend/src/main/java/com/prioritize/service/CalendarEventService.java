package com.prioritize.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.CalendarEventRequest;
import com.prioritize.dto.CalendarEventResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.CalendarEventMapper;
import com.prioritize.model.CalendarEvent;
import com.prioritize.repository.CalendarEventRepository;
import com.prioritize.repository.CategoryRepository;

@Service
@Transactional
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final CategoryRepository categoryRepository;
    private final CalendarEventMapper calendarEventMapper;

    public CalendarEventService(
            CalendarEventRepository calendarEventRepository,
            CategoryRepository categoryRepository,
            CalendarEventMapper calendarEventMapper) {
        this.calendarEventRepository = calendarEventRepository;
        this.categoryRepository = categoryRepository;
        this.calendarEventMapper = calendarEventMapper;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> list(UUID userId, Instant from, Instant to) {
        validateWindowParams(from, to);
        List<CalendarEvent> events = (from != null && to != null)
                ? calendarEventRepository.findOverlapping(userId, from, to)
                : calendarEventRepository.findByUserIdOrderByStartAtAsc(userId);
        return events.stream().map(calendarEventMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CalendarEventResponse get(UUID userId, UUID eventId) {
        return calendarEventMapper.toResponse(requireOwned(userId, eventId));
    }

    public CalendarEventResponse create(UUID userId, CalendarEventRequest request) {
        validateRange(request.startAt(), request.endAt());
        validateCategory(userId, request.categoryId());
        CalendarEvent event = new CalendarEvent();
        event.setUserId(userId);
        calendarEventMapper.applyCreate(event, request);
        return calendarEventMapper.toResponse(calendarEventRepository.save(event));
    }

    public CalendarEventResponse update(UUID userId, UUID eventId, CalendarEventRequest request) {
        CalendarEvent event = requireOwned(userId, eventId);
        validateRange(request.startAt(), request.endAt());
        validateCategory(userId, request.categoryId());
        calendarEventMapper.applyUpdate(event, request);
        return calendarEventMapper.toResponse(calendarEventRepository.save(event));
    }

    public void delete(UUID userId, UUID eventId) {
        CalendarEvent event = requireOwned(userId, eventId);
        calendarEventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public CalendarEvent requireOwned(UUID userId, UUID eventId) {
        return calendarEventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found"));
    }

    private void validateCategory(UUID userId, UUID categoryId) {
        if (categoryId != null) {
            categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }
    }

    private void validateRange(Instant startAt, Instant endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endAt must be after startAt");
        }
    }

    private void validateWindowParams(Instant from, Instant to) {
        if ((from == null) != (to == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from and to must both be provided or both omitted");
        }
        if (from != null && !to.isAfter(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "to must be after from");
        }
    }
}
