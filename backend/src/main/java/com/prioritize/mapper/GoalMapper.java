package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.GoalRequest;
import com.prioritize.dto.GoalResponse;
import com.prioritize.model.Goal;
import com.prioritize.model.GoalStatus;

@Component
public class GoalMapper {

    public GoalResponse toResponse(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getCategoryId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }

    public void applyCreate(Goal goal, GoalRequest request) {
        goal.setCategoryId(request.categoryId());
        goal.setTitle(request.title().trim());
        goal.setDescription(blankToNull(request.description()));
        goal.setTargetDate(request.targetDate());
        goal.setStatus(request.status() != null ? request.status() : GoalStatus.ACTIVE);
    }

    public void applyUpdate(Goal goal, GoalRequest request) {
        goal.setCategoryId(request.categoryId());
        goal.setTitle(request.title().trim());
        goal.setDescription(blankToNull(request.description()));
        goal.setTargetDate(request.targetDate());
        if (request.status() != null) {
            goal.setStatus(request.status());
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
