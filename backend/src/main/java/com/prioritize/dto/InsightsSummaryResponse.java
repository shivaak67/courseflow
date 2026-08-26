package com.prioritize.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InsightsSummaryResponse(
        Instant from,
        Instant to,
        int tasksCreated,
        int tasksCompleted,
        int openTasks,
        int totalMinutesLogged,
        int estimatedMinutesOpen,
        double completionRate,
        List<MinutesByDay> minutesByDay,
        List<TaskMinutes> topTasksByMinutes) {

    public record MinutesByDay(LocalDate date, int minutes) {
    }

    public record TaskMinutes(UUID taskId, String title, int minutes) {
    }
}
