package com.prioritize.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.config.CanvasProperties;
import com.prioritize.dto.CanvasSyncResponse;
import com.prioritize.dto.PriorityInput;
import com.prioritize.dto.PriorityResult;
import com.prioritize.exception.ApiException;
import com.prioritize.integration.canvas.CanvasAssignmentData;
import com.prioritize.integration.canvas.CanvasClient;
import com.prioritize.integration.canvas.CanvasCourseData;
import com.prioritize.model.Assignment;
import com.prioritize.model.Course;
import com.prioritize.model.PriorityLevel;
import com.prioritize.repository.AssignmentRepository;
import com.prioritize.repository.CourseRepository;

@Service
public class CanvasSyncService {

    private final CanvasClient canvasClient;
    private final CanvasProperties canvasProperties;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final PriorityService priorityService;

    public CanvasSyncService(
            CanvasClient canvasClient,
            CanvasProperties canvasProperties,
            CourseRepository courseRepository,
            AssignmentRepository assignmentRepository,
            PriorityService priorityService) {
        this.canvasClient = canvasClient;
        this.canvasProperties = canvasProperties;
        this.courseRepository = courseRepository;
        this.assignmentRepository = assignmentRepository;
        this.priorityService = priorityService;
    }

    @Transactional
    public CanvasSyncResponse sync(UUID userId) {
        if (!canvasProperties.isMockEnabled() && !canvasProperties.isConfigured()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Canvas is not configured. Set CANVAS_BASE_URL and CANVAS_API_TOKEN, or enable mock mode.");
        }

        int coursesUpserted = 0;
        int assignmentsUpserted = 0;

        for (CanvasCourseData canvasCourse : canvasClient.listCourses()) {
            Course course = upsertCourse(userId, canvasCourse);
            coursesUpserted++;

            for (CanvasAssignmentData canvasAssignment : canvasClient.listAssignments(canvasCourse.canvasCourseId())) {
                upsertAssignment(userId, course, canvasAssignment);
                assignmentsUpserted++;
            }
        }

        return new CanvasSyncResponse(coursesUpserted, assignmentsUpserted, Instant.now());
    }

    private Course upsertCourse(UUID userId, CanvasCourseData data) {
        Course course = courseRepository
                .findByUserIdAndCanvasCourseId(userId, data.canvasCourseId())
                .orElseGet(Course::new);

        if (course.getId() == null) {
            course.setUserId(userId);
            course.setCanvasCourseId(data.canvasCourseId());
        }

        course.setName(data.name());
        course.setCourseCode(data.courseCode());
        course.setTerm(data.term());
        return courseRepository.save(course);
    }

    private Assignment upsertAssignment(UUID userId, Course course, CanvasAssignmentData data) {
        Assignment assignment = assignmentRepository
                .findByUserIdAndCanvasAssignmentId(userId, data.canvasAssignmentId())
                .orElseGet(Assignment::new);

        boolean isNew = assignment.getId() == null;
        if (isNew) {
            assignment.setUserId(userId);
            assignment.setCanvasAssignmentId(data.canvasAssignmentId());
            assignment.setActualHours(0.0);
        }

        assignment.setCourse(course);
        assignment.setTitle(data.title());
        assignment.setDescription(data.description());
        assignment.setDueDate(data.dueAt());
        assignment.setPointsPossible(data.pointsPossible());
        assignment.setSubmitted(data.submitted());
        if (data.completed() || isNew) {
            assignment.setCompleted(data.completed());
        }

        applyPriority(assignment);
        return assignmentRepository.save(assignment);
    }

    private void applyPriority(Assignment assignment) {
        PriorityResult result = priorityService.calculate(new PriorityInput(
                assignment.getDueDate(),
                toBigDecimal(assignment.getPointsPossible()),
                null,
                assignment.getDifficulty() != null ? assignment.getDifficulty().name() : null,
                toBigDecimal(assignment.getEstimatedHours()),
                assignment.getPersonalPriority(),
                assignment.isCompleted(),
                assignment.isSubmitted()));
        assignment.setPriorityScore(result.score());
        assignment.setPriorityLevel(PriorityLevel.valueOf(result.level().name()));
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
