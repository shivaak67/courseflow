package com.prioritize.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prioritize.config.CanvasProperties;
import com.prioritize.dto.CanvasSyncResponse;
import com.prioritize.dto.PriorityLevel;
import com.prioritize.dto.PriorityResult;
import com.prioritize.exception.ApiException;
import com.prioritize.integration.canvas.CanvasAssignmentData;
import com.prioritize.integration.canvas.CanvasClient;
import com.prioritize.integration.canvas.CanvasCourseData;
import com.prioritize.model.Assignment;
import com.prioritize.model.Course;
import com.prioritize.repository.AssignmentRepository;
import com.prioritize.repository.CourseRepository;
import com.prioritize.service.CanvasSyncService;
import com.prioritize.service.PriorityService;

@ExtendWith(MockitoExtension.class)
class CanvasSyncServiceTest {

    @Mock
    private CanvasClient canvasClient;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private PriorityService priorityService;

    private CanvasProperties canvasProperties;
    private CanvasSyncService canvasSyncService;

    @BeforeEach
    void setUp() {
        canvasProperties = new CanvasProperties();
        canvasProperties.setMockEnabled(true);
        canvasSyncService = new CanvasSyncService(
                canvasClient,
                canvasProperties,
                courseRepository,
                assignmentRepository,
                priorityService);
    }

    @Test
    void syncUpsertsCoursesAndAssignments() {
        UUID userId = UUID.randomUUID();
        when(canvasClient.listCourses()).thenReturn(List.of(
                new CanvasCourseData("c1", "Algorithms", "CS 310", "Fall 2026")));
        when(canvasClient.listAssignments("c1")).thenReturn(List.of(
                new CanvasAssignmentData(
                        "a1",
                        "HW1",
                        "Do the homework",
                        Instant.parse("2026-09-01T23:59:00Z"),
                        100.0,
                        false,
                        false)));
        when(courseRepository.findByUserIdAndCanvasCourseId(userId, "c1")).thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            if (course.getId() == null) {
                course.setId(UUID.randomUUID());
            }
            return course;
        });
        when(assignmentRepository.findByUserIdAndCanvasAssignmentId(userId, "a1")).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(priorityService.calculate(any())).thenReturn(new PriorityResult(72.0, PriorityLevel.HIGH, List.of()));

        CanvasSyncResponse response = canvasSyncService.sync(userId);

        assertThat(response.coursesUpserted()).isEqualTo(1);
        assertThat(response.assignmentsUpserted()).isEqualTo(1);
        assertThat(response.lastSyncedAt()).isNotNull();

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        Assignment saved = assignmentCaptor.getValue();
        assertThat(saved.getCanvasAssignmentId()).isEqualTo("a1");
        assertThat(saved.getTitle()).isEqualTo("HW1");
        assertThat(saved.getPriorityScore()).isEqualTo(72.0);
    }

    @Test
    void syncPreservesStudentCompletedUnlessCanvasGraded() {
        UUID userId = UUID.randomUUID();
        Course existingCourse = new Course();
        existingCourse.setId(UUID.randomUUID());
        existingCourse.setUserId(userId);
        existingCourse.setCanvasCourseId("c1");

        Assignment existing = new Assignment();
        existing.setId(UUID.randomUUID());
        existing.setUserId(userId);
        existing.setCourse(existingCourse);
        existing.setCanvasAssignmentId("a1");
        existing.setTitle("Old");
        existing.setCompleted(true);
        existing.setSubmitted(false);
        existing.setActualHours(2.5);
        existing.setPersonalPriority(4);

        when(canvasClient.listCourses()).thenReturn(List.of(
                new CanvasCourseData("c1", "Algorithms", "CS 310", "Fall 2026")));
        when(canvasClient.listAssignments("c1")).thenReturn(List.of(
                new CanvasAssignmentData("a1", "HW1 Updated", null, null, 80.0, true, false)));
        when(courseRepository.findByUserIdAndCanvasCourseId(userId, "c1")).thenReturn(Optional.of(existingCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignmentRepository.findByUserIdAndCanvasAssignmentId(userId, "a1")).thenReturn(Optional.of(existing));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(priorityService.calculate(any())).thenReturn(new PriorityResult(40.0, PriorityLevel.MEDIUM, List.of()));

        canvasSyncService.sync(userId);

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        Assignment saved = assignmentCaptor.getValue();
        assertThat(saved.getTitle()).isEqualTo("HW1 Updated");
        assertThat(saved.isCompleted()).isTrue();
        assertThat(saved.isSubmitted()).isTrue();
        assertThat(saved.getActualHours()).isEqualTo(2.5);
        assertThat(saved.getPersonalPriority()).isEqualTo(4);
    }

    @Test
    void syncRequiresConfigWhenMockDisabled() {
        canvasProperties.setMockEnabled(false);
        canvasProperties.setBaseUrl("");
        canvasProperties.setApiToken("");

        assertThatThrownBy(() -> canvasSyncService.sync(UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Canvas is not configured");

        verify(canvasClient, never()).listCourses();
    }
}
