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
import org.springframework.web.bind.annotation.RestController;

import com.prioritize.dto.CourseRequest;
import com.prioritize.dto.CourseResponse;
import com.prioritize.security.CurrentUserService;
import com.prioritize.service.CourseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final CurrentUserService currentUserService;

    public CourseController(CourseService courseService, CurrentUserService currentUserService) {
        this.courseService = courseService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<CourseResponse> list() {
        UUID userId = currentUserService.requireCurrentUserId();
        return courseService.list(userId);
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        CourseResponse created = courseService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        return courseService.get(userId, id);
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable UUID id, @Valid @RequestBody CourseRequest request) {
        UUID userId = currentUserService.requireCurrentUserId();
        return courseService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID userId = currentUserService.requireCurrentUserId();
        courseService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
