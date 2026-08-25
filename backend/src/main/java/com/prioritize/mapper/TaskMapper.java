package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.TaskRequest;
import com.prioritize.dto.TaskResponse;
import com.prioritize.model.Task;
import com.prioritize.model.TaskPriority;
import com.prioritize.model.TaskStatus;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getCategoryId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getDueTime(),
                task.getEstimatedMinutes(),
                task.getActualMinutes(),
                task.getPriority(),
                task.getStatus(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    public void applyCreate(Task task, TaskRequest request) {
        applyFields(task, request);
        if (task.getPriority() == null) {
            task.setPriority(TaskPriority.MEDIUM);
        }
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        // actualMinutes stays at entity default 0 — time tracking is separate
    }

    public void applyUpdate(Task task, TaskRequest request) {
        applyFields(task, request);
        if (task.getPriority() == null) {
            task.setPriority(TaskPriority.MEDIUM);
        }
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        // do not modify actualMinutes
    }

    private void applyFields(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setCategoryId(request.categoryId());
        task.setProjectId(request.projectId());
        task.setDueDate(request.dueDate());
        task.setDueTime(request.dueTime());
        task.setEstimatedMinutes(request.estimatedMinutes());
        task.setPriority(request.priority());
        task.setStatus(request.status());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
