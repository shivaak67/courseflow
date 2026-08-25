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

import com.prioritize.dto.ScheduleBlockRequest;
import com.prioritize.dto.ScheduleBlockResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.ScheduleBlockService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schedule-blocks")
public class ScheduleBlockController {

    private final ScheduleBlockService scheduleBlockService;
    private final CurrentUserService currentUserService;

    public ScheduleBlockController(
            ScheduleBlockService scheduleBlockService, CurrentUserService currentUserService) {
        this.scheduleBlockService = scheduleBlockService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ScheduleBlockResponse> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        UUID userId = currentUserService.requireCurrentUserId();
        return scheduleBlockService.list(userId, from, to);
    }

    @PostMapping
    public ResponseEntity<ScheduleBlockResponse> create(@Valid @RequestBody ScheduleBlockRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        ScheduleBlockResponse created = scheduleBlockService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ScheduleBlockResponse get(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return scheduleBlockService.get(userId, id);
    }

    @PutMapping("/{id}")
    public ScheduleBlockResponse update(
            @PathVariable UUID id, @Valid @RequestBody ScheduleBlockRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return scheduleBlockService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        scheduleBlockService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
