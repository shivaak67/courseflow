package com.prioritize.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.CalendarEventRequest;
import com.prioritize.dto.CalendarEventResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.CalendarEventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/calendar-events")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;
    private final CurrentUserService currentUserService;

    public CalendarEventController(
            CalendarEventService calendarEventService, CurrentUserService currentUserService) {
        this.calendarEventService = calendarEventService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<CalendarEventResponse> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        UUID userId = currentUserService.requireCurrentUserId();
        return calendarEventService.list(userId, from, to);
    }

    @PostMapping
    public ResponseEntity<CalendarEventResponse> create(@Valid @RequestBody CalendarEventRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        CalendarEventResponse created = calendarEventService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public CalendarEventResponse get(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return calendarEventService.get(userId, id);
    }

    @PutMapping("/{id}")
    public CalendarEventResponse update(
            @PathVariable UUID id, @Valid @RequestBody CalendarEventRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return calendarEventService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        calendarEventService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
