package com.prioritize.controller;

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

import com.prioritize.dto.TimeEntryRequest;
import com.prioritize.dto.TimeEntryResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.TimeEntryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/time-entries")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final CurrentUserService currentUserService;

    public TimeEntryController(TimeEntryService timeEntryService, CurrentUserService currentUserService) {
        this.timeEntryService = timeEntryService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<TimeEntryResponse> list(@RequestParam(required = false) UUID taskId) {
        UUID userId = currentUserService.requireCurrentUserId();
        return timeEntryService.list(userId, taskId);
    }

    @PostMapping
    public ResponseEntity<TimeEntryResponse> create(@Valid @RequestBody TimeEntryRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        TimeEntryResponse created = timeEntryService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public TimeEntryResponse get(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return timeEntryService.get(userId, id);
    }

    @PutMapping("/{id}")
    public TimeEntryResponse update(@PathVariable UUID id, @Valid @RequestBody TimeEntryRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return timeEntryService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        timeEntryService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
