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

import com.prioritize.dto.ReminderRequest;
import com.prioritize.dto.ReminderResponse;
import com.prioritize.dto.ReminderUpdateRequest;
import com.prioritize.model.ReminderStatus;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.ReminderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final CurrentUserService currentUserService;

    public ReminderController(ReminderService reminderService, CurrentUserService currentUserService) {
        this.reminderService = reminderService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ReminderResponse> list(@RequestParam(required = false) ReminderStatus status) {
        UUID userId = currentUserService.requireCurrentUserId();
        return reminderService.list(userId, status);
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> create(@Valid @RequestBody ReminderRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        ReminderResponse created = reminderService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ReminderResponse get(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return reminderService.get(userId, id);
    }

    @PutMapping("/{id}")
    public ReminderResponse update(@PathVariable UUID id, @Valid @RequestBody ReminderUpdateRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return reminderService.update(userId, id, request);
    }

    @PostMapping("/{id}/cancel")
    public ReminderResponse cancel(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return reminderService.cancel(userId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        reminderService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
