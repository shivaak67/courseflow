package com.prioritize.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.AssignmentCreateRequest;
import com.prioritize.dto.AssignmentResponse;
import com.prioritize.dto.AssignmentUpdateRequest;
import com.prioritize.dto.PrioritizedAssignmentResponse;
import com.prioritize.dto.PriorityInput;
import com.prioritize.dto.PriorityResult;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.AssignmentMapper;
import com.prioritize.model.Assignment;
import com.prioritize.model.Course;
import com.prioritize.model.PriorityLevel;
import com.prioritize.repository.AssignmentRepository;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseService courseService;
    private final AssignmentMapper assignmentMapper;
    private final PriorityService priorityService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            CourseService courseService,
            AssignmentMapper assignmentMapper,
            PriorityService priorityService) {
        this.assignmentRepository = assignmentRepository;
        this.courseService = courseService;
        this.assignmentMapper = assignmentMapper;
        this.priorityService = priorityService;
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
        applyPriority(assignment);
        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    public AssignmentResponse update(UUID userId, UUID assignmentId, AssignmentUpdateRequest request) {
        Assignment assignment = requireOwned(userId, assignmentId);
        assignmentMapper.applyUpdate(assignment, request);
        applyPriority(assignment);
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

    public List<PrioritizedAssignmentResponse> prioritized(UUID userId) {
        return assignmentRepository.findOpenOrderedByDueDate(userId).stream()
                .map(assignment -> {
                    PriorityResult result = applyPriority(assignment);
                    Assignment saved = assignmentRepository.save(assignment);
                    return assignmentMapper.toPrioritizedResponse(saved, result.reasons());
                })
                .sorted(Comparator.comparing(
                        PrioritizedAssignmentResponse::priorityScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private PriorityResult applyPriority(Assignment assignment) {
        PriorityResult result = priorityService.calculate(toPriorityInput(assignment));
        assignment.setPriorityScore(result.score());
        assignment.setPriorityLevel(toModelLevel(result.level()));
        return result;
    }

    private PriorityInput toPriorityInput(Assignment assignment) {
        return new PriorityInput(
                assignment.getDueDate(),
                toBigDecimal(assignment.getPointsPossible()),
                null,
                assignment.getDifficulty() != null ? assignment.getDifficulty().name() : null,
                toBigDecimal(assignment.getEstimatedHours()),
                assignment.getPersonalPriority(),
                assignment.isCompleted(),
                assignment.isSubmitted());
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private static PriorityLevel toModelLevel(com.prioritize.dto.PriorityLevel level) {
        return PriorityLevel.valueOf(level.name());
    }

    private Assignment requireOwned(UUID userId, UUID assignmentId) {
        return assignmentRepository.findByIdAndUserId(assignmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    }
}
