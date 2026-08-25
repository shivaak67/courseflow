package com.prioritize.controller;

import java.time.LocalDate;
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

import com.prioritize.dto.OccurrenceResponse;
import com.prioritize.dto.RoutineRequest;
import com.prioritize.dto.RoutineResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.RoutineService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/routines")
public class RoutineController {

    private final RoutineService routineService;
    private final CurrentUserService currentUserService;

    public RoutineController(RoutineService routineService, CurrentUserService currentUserService) {
        this.routineService = routineService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<RoutineResponse> list(@RequestParam(required = false) Boolean active) {
        UUID userId = currentUserService.requireCurrentUserId();
        return routineService.list(userId, active);
    }

    @GetMapping("/occurrences")
    public List<OccurrenceResponse> occurrences(
            @RequestParam LocalDate from, @RequestParam LocalDate to) {
        UUID userId = currentUserService.requireCurrentUserId();
        return routineService.occurrences(userId, from, to);
    }

    @PostMapping
    public ResponseEntity<RoutineResponse> create(@Valid @RequestBody RoutineRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        RoutineResponse created = routineService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public RoutineResponse get(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return routineService.get(userId, id);
    }

    @PutMapping("/{id}")
    public RoutineResponse update(
            @PathVariable UUID id, @Valid @RequestBody RoutineRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return routineService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        routineService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
