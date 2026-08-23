package com.prioritize.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.AssignmentCreateRequest;
import com.prioritize.dto.AssignmentResponse;
import com.prioritize.dto.AssignmentUpdateRequest;
import com.prioritize.dto.PrioritizedAssignmentResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.AssignmentMapper;
import com.prioritize.model.Assignment;
import com.prioritize.model.Course;
import com.prioritize.repository.AssignmentRepository;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseService courseService;
    private final AssignmentMapper assignmentMapper;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            CourseService courseService,
            AssignmentMapper assignmentMapper) {
        this.assignmentRepository = assignmentRepository;
        this.courseService = courseService;
        this.assignmentMapper = assignmentMapper;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(UUID userId, UUID courseId, Boolean completed) {
        return assignmentRepository.findFiltered(userId, courseId, completed).stream()
                .map(assignmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(UUID userId, UUID assignmentId) {
        return assignmentMapper.toResponse(requireOwned(userId, assignmentId));
    }

    public AssignmentResponse create(UUID userId, AssignmentCreateRequest request) {
        Course course = courseService.requireOwned(userId, request.courseId());
        Assignment assignment = new Assignment();
        assignmentMapper.applyCreate(assignment, request, course);
        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    public AssignmentResponse update(UUID userId, UUID assignmentId, AssignmentUpdateRequest request) {
        Assignment assignment = requireOwned(userId, assignmentId);
        assignmentMapper.applyUpdate(assignment, request);
        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> upcoming(UUID userId) {
        return assignmentRepository.findUpcoming(userId, Instant.now()).stream()
                .map(assignmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> overdue(UUID userId) {
        return assignmentRepository.findOverdue(userId, Instant.now()).stream()
                .map(assignmentMapper::toResponse)
                .toList();
    }

    /**
     * Stub until Agent C implements the priority engine.
     * Returns open assignments ordered by due date with empty reasons.
     */
    @Transactional(readOnly = true)
    public List<PrioritizedAssignmentResponse> prioritized(UUID userId) {
        return assignmentRepository.findOpenOrderedByDueDate(userId).stream()
                .map(assignment -> assignmentMapper.toPrioritizedResponse(assignment, Collections.emptyList()))
                .toList();
    }

    private Assignment requireOwned(UUID userId, UUID assignmentId) {
        return assignmentRepository.findByIdAndUserId(assignmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    }
}
