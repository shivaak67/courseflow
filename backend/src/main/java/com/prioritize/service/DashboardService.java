package com.prioritize.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.DashboardSummaryResponse;
import com.prioritize.dto.PriorityInput;
import com.prioritize.dto.PriorityLevel;
import com.prioritize.dto.PriorityResult;
import com.prioritize.model.Assignment;
import com.prioritize.repository.AssignmentRepository;

@Service
public class DashboardService {

    private final AssignmentRepository assignmentRepository;
    private final PriorityService priorityService;
    private final Clock clock;

    public DashboardService(
            AssignmentRepository assignmentRepository,
            PriorityService priorityService,
            Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.priorityService = priorityService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(UUID userId) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant startOfToday = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfToday = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfWeek = today.plusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Assignment> assignments = assignmentRepository.findFiltered(userId, null, null);

        int dueToday = 0;
        int dueThisWeek = 0;
        int overdue = 0;
        int highPriority = 0;
        int completed = 0;
        int remaining = 0;
        double hoursThisWeek = 0.0;

        Map<UUID, DashboardSummaryResponse.WorkloadByCourse> workload = new LinkedHashMap<>();

        for (Assignment assignment : assignments) {
            if (assignment.isCompleted()) {
                completed++;
            }

            boolean open = !assignment.isCompleted() && !assignment.isSubmitted();
            if (!open) {
                continue;
            }

            remaining++;
            PriorityResult priority = priorityService.calculate(toPriorityInput(assignment));
            if (priority.level() == PriorityLevel.HIGH || priority.level() == PriorityLevel.CRITICAL) {
                highPriority++;
            }

            Instant due = assignment.getDueDate();
            if (due != null) {
                if (!due.isBefore(startOfToday) && due.isBefore(endOfToday)) {
                    dueToday++;
                }
                if (due.isBefore(now)) {
                    overdue++;
                }
                if (!due.isBefore(startOfToday) && due.isBefore(endOfWeek)) {
                    dueThisWeek++;
                    hoursThisWeek += hoursOrZero(assignment.getEstimatedHours());
                }
            }

            UUID courseId = assignment.getCourse().getId();
            DashboardSummaryResponse.WorkloadByCourse existing = workload.get(courseId);
            if (existing == null) {
                workload.put(
                        courseId,
                        new DashboardSummaryResponse.WorkloadByCourse(
                                courseId,
                                assignment.getCourse().getName(),
                                1,
                                hoursOrZero(assignment.getEstimatedHours())));
            } else {
                workload.put(
                        courseId,
                        new DashboardSummaryResponse.WorkloadByCourse(
                                existing.courseId(),
                                existing.courseName(),
                                existing.assignmentCount() + 1,
                                existing.estimatedHours() + hoursOrZero(assignment.getEstimatedHours())));
            }
        }

        List<DashboardSummaryResponse.WorkloadByCourse> workloadByCourse = workload.values().stream()
                .sorted(Comparator.comparing(DashboardSummaryResponse.WorkloadByCourse::estimatedHours).reversed())
                .toList();

        return new DashboardSummaryResponse(
                dueToday,
                dueThisWeek,
                overdue,
                highPriority,
                completed,
                remaining,
                roundHours(hoursThisWeek),
                workloadByCourse);
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

    private static double hoursOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double roundHours(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
