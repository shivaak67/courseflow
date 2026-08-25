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

import com.prioritize.dto.GoalRequest;
import com.prioritize.dto.GoalResponse;
import com.prioritize.model.GoalStatus;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.GoalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final CurrentUserService currentUserService;

    public GoalController(GoalService goalService, CurrentUserService currentUserService) {
        this.goalService = goalService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<GoalResponse> list(@RequestParam(required = false) GoalStatus status) {
        UUID userId = currentUserService.requireCurrentUserId();
        return goalService.list(userId, status);
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        GoalResponse created = goalService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public GoalResponse get(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return goalService.get(userId, id);
    }

    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable UUID id, @Valid @RequestBody GoalRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return goalService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        goalService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
