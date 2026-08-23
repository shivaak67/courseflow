package com.prioritize.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.AssignmentCreateRequest;
import com.prioritize.dto.AssignmentResponse;
import com.prioritize.dto.AssignmentUpdateRequest;
import com.prioritize.dto.PrioritizedAssignmentResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.AssignmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final CurrentUserService currentUserService;

    public AssignmentController(AssignmentService assignmentService, CurrentUserService currentUserService) {
        this.assignmentService = assignmentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AssignmentResponse> list(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) Boolean completed) {
        UUID userId = currentUserService.requireCurrentUserId();
        return assignmentService.list(userId, courseId, completed);
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody AssignmentCreateRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        AssignmentResponse created = assignmentService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/upcoming")
    public List<AssignmentResponse> upcoming() {
        UUID userId = currentUserService.requireCurrentUserId();
        return assignmentService.upcoming(userId);
    }

    @GetMapping("/overdue")
    public List<AssignmentResponse> overdue() {
        UUID userId = currentUserService.requireCurrentUserId();
        return assignmentService.overdue(userId);
    }

    @GetMapping("/prioritized")
    public List<PrioritizedAssignmentResponse> prioritized() {
        UUID userId = currentUserService.requireCurrentUserId();
        return assignmentService.prioritized(userId);
    }

    @GetMapping("/{id}")
    public AssignmentResponse get(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return assignmentService.get(userId, id);
    }

    @PutMapping("/{id}")
    public AssignmentResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody AssignmentUpdateRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return assignmentService.update(userId, id, request);
    }
}
