package com.prioritize.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prioritize.dto.DashboardSummaryResponse;
import com.prioritize.dto.PriorityLevel;
import com.prioritize.dto.PriorityResult;
import com.prioritize.model.Assignment;
import com.prioritize.model.Course;
import com.prioritize.repository.AssignmentRepository;
import com.prioritize.service.DashboardService;
import com.prioritize.service.PriorityService;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private PriorityService priorityService;

    private DashboardService dashboardService;
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-10T15:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(assignmentRepository, priorityService, clock);
    }

    @Test
    void summaryAggregatesOpenAssignments() {
        UUID userId = UUID.randomUUID();
        Course course = course("Algorithms");
        Assignment dueToday = openAssignment(course, "HW1", Instant.parse("2026-09-10T20:00:00Z"), 3.0);
        Assignment overdue = openAssignment(course, "HW0", Instant.parse("2026-09-08T20:00:00Z"), 2.0);
        Assignment later = openAssignment(course, "Project", Instant.parse("2026-09-20T20:00:00Z"), 8.0);
        Assignment done = openAssignment(course, "Done", Instant.parse("2026-09-10T12:00:00Z"), 1.0);
        done.setCompleted(true);

        when(assignmentRepository.findFiltered(eq(userId), eq(null), eq(null)))
                .thenReturn(List.of(dueToday, overdue, later, done));
        when(priorityService.calculate(any()))
                .thenReturn(new PriorityResult(85.0, PriorityLevel.CRITICAL, List.of("Due soon")))
                .thenReturn(new PriorityResult(70.0, PriorityLevel.HIGH, List.of("Overdue")))
                .thenReturn(new PriorityResult(40.0, PriorityLevel.MEDIUM, List.of()));

        DashboardSummaryResponse summary = dashboardService.summary(userId);

        assertThat(summary.dueTodayCount()).isEqualTo(1);
        assertThat(summary.dueThisWeekCount()).isEqualTo(1);
        assertThat(summary.overdueCount()).isEqualTo(1);
        assertThat(summary.highPriorityCount()).isEqualTo(2);
        assertThat(summary.completedCount()).isEqualTo(1);
        assertThat(summary.remainingCount()).isEqualTo(3);
        assertThat(summary.estimatedHoursRemainingThisWeek()).isEqualTo(3.0);
        assertThat(summary.workloadByCourse()).hasSize(1);
        assertThat(summary.workloadByCourse().getFirst().assignmentCount()).isEqualTo(3);
        assertThat(summary.workloadByCourse().getFirst().estimatedHours()).isEqualTo(13.0);
    }

    private static Course course(String name) {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setName(name);
        return course;
    }

    private static Assignment openAssignment(Course course, String title, Instant due, double hours) {
        Assignment assignment = new Assignment();
        assignment.setId(UUID.randomUUID());
        assignment.setCourse(course);
        assignment.setTitle(title);
        assignment.setDueDate(due);
        assignment.setEstimatedHours(hours);
        assignment.setCompleted(false);
        assignment.setSubmitted(false);
        return assignment;
    }
}
