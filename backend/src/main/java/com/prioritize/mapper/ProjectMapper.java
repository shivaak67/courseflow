package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.ProjectRequest;
import com.prioritize.dto.ProjectResponse;
import com.prioritize.model.Project;
import com.prioritize.model.ProjectStatus;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getCategoryId(),
                project.getGoalId(),
                project.getTitle(),
                project.getDescription(),
                project.getStartDate(),
                project.getTargetDate(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    public void applyCreate(Project project, ProjectRequest request) {
        applyFields(project, request);
        if (project.getStatus() == null) {
            project.setStatus(ProjectStatus.ACTIVE);
        }
    }

    public void applyUpdate(Project project, ProjectRequest request) {
        applyFields(project, request);
        if (project.getStatus() == null) {
            project.setStatus(ProjectStatus.ACTIVE);
        }
    }

    private void applyFields(Project project, ProjectRequest request) {
        project.setTitle(request.title().trim());
        project.setDescription(blankToNull(request.description()));
        project.setCategoryId(request.categoryId());
        project.setGoalId(request.goalId());
        project.setStartDate(request.startDate());
        project.setTargetDate(request.targetDate());
        project.setStatus(request.status());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
