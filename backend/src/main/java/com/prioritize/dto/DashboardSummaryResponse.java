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
        List<WorkloadByProject> workloadByProject) {

    public record WorkloadByProject(
            UUID projectId,
            String projectName,
            int taskCount,
            double estimatedHours) {
    }
}
