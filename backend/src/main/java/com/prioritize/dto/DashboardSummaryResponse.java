package com.prioritize.dto;

import java.util.List;
import java.util.UUID;

public record DashboardSummaryResponse(
        int dueTodayCount,
        int dueThisWeekCount,
        int overdueCount,
        int highPriorityCount,
        int completedCount,
        int remainingCount,
        double estimatedHoursRemainingThisWeek,
        List<WorkloadByCourse> workloadByCourse) {

    public record WorkloadByCourse(
            UUID courseId,
            String courseName,
            int assignmentCount,
            double estimatedHours) {
    }
}
