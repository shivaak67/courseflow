package com.prioritize.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.StudySessionCreateRequest;
import com.prioritize.dto.StudySessionResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.StudySessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/study-sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;
    private final CurrentUserService currentUserService;

    public StudySessionController(
            StudySessionService studySessionService, CurrentUserService currentUserService) {
        this.studySessionService = studySessionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<StudySessionResponse> create(@Valid @RequestBody StudySessionCreateRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        StudySessionResponse created = studySessionService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<StudySessionResponse> list() {
        UUID userId = currentUserService.requireCurrentUserId();
        return studySessionService.list(userId);
    }
}
