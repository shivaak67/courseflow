package com.prioritize.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.InsightsSummaryResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.model.TaskStatus;
import com.prioritize.repository.TaskRepository;
import com.prioritize.repository.TimeEntryRepository;

/**
 * Read-only analytics: aggregate counts and minutes only — no recommendations or auto-priority.
 */
@Service
@Transactional(readOnly = true)
public class InsightsService {

    private static final int MAX_WINDOW_DAYS = 366;
    private static final int TOP_TASKS_LIMIT = 5;
    private static final Set<TaskStatus> OPEN_STATUSES =
            EnumSet.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

    private final TaskRepository taskRepository;
    private final TimeEntryRepository timeEntryRepository;

    public InsightsService(TaskRepository taskRepository, TimeEntryRepository timeEntryRepository) {
        this.taskRepository = taskRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    public InsightsSummaryResponse summary(UUID userId, Instant from, Instant to) {
        validateWindow(from, to);

        // Half-open window [from, to) for created/completed/logged aggregations.
        int tasksCreated = toInt(taskRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId, from, to));
        int tasksCompleted =
                toInt(taskRepository.countByUserIdAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        userId, from, to));

        // openTasks / estimatedMinutesOpen: current snapshot (not bound to the from/to window).
        int openTasks = toInt(taskRepository.countByUserIdAndStatusIn(userId, OPEN_STATUSES));
        int estimatedMinutesOpen =
                toInt(taskRepository.sumEstimatedMinutesByUserIdAndStatusIn(userId, OPEN_STATUSES));

        int totalMinutesLogged =
                toInt(timeEntryRepository.sumDurationByUserIdAndCreatedAtInWindow(userId, from, to));

        double completionRate = (double) tasksCompleted / Math.max(tasksCreated, 1);

        List<InsightsSummaryResponse.MinutesByDay> minutesByDay = buildMinutesByDay(userId, from, to);
        List<InsightsSummaryResponse.TaskMinutes> topTasks = buildTopTasks(userId, from, to);

        return new InsightsSummaryResponse(
                from,
                to,
                tasksCreated,
                tasksCompleted,
                openTasks,
                totalMinutesLogged,
                estimatedMinutesOpen,
                completionRate,
                minutesByDay,
                topTasks);
    }

    private List<InsightsSummaryResponse.MinutesByDay> buildMinutesByDay(
            UUID userId, Instant from, Instant to) {
        List<Object[]> rows = timeEntryRepository.findCreatedAtAndDurationInWindow(userId, from, to);
        Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Instant createdAt = (Instant) row[0];
            int minutes = ((Number) row[1]).intValue();
            LocalDate day = LocalDate.ofInstant(createdAt, ZoneOffset.UTC);
            byDay.merge(day, minutes, Integer::sum);
        }
        List<InsightsSummaryResponse.MinutesByDay> result = new ArrayList<>(byDay.size());
        byDay.forEach((date, minutes) -> result.add(new InsightsSummaryResponse.MinutesByDay(date, minutes)));
        return result;
    }

    private List<InsightsSummaryResponse.TaskMinutes> buildTopTasks(
            UUID userId, Instant from, Instant to) {
        List<Object[]> rows = timeEntryRepository.findTopTasksByMinutesInWindow(
                userId, from, to, PageRequest.of(0, TOP_TASKS_LIMIT));
        List<InsightsSummaryResponse.TaskMinutes> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            UUID taskId = (UUID) row[0];
            String title = (String) row[1];
            int minutes = ((Number) row[2]).intValue();
            result.add(new InsightsSummaryResponse.TaskMinutes(taskId, title, minutes));
        }
        return result;
    }

    private void validateWindow(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from and to are required");
        }
        if (!to.isAfter(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "to must be after from");
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days > MAX_WINDOW_DAYS) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "insights window cannot exceed 366 days");
        }
    }

    private static int toInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }
}
