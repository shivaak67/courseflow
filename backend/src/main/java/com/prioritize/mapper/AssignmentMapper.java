package com.prioritize.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.prioritize.dto.AssignmentCreateRequest;
import com.prioritize.dto.AssignmentResponse;
import com.prioritize.dto.AssignmentUpdateRequest;
import com.prioritize.dto.PrioritizedAssignmentResponse;
import com.prioritize.model.Assignment;
import com.prioritize.model.Course;

@Component
public class AssignmentMapper {

    public AssignmentResponse toResponse(Assignment assignment) {
        Course course = assignment.getCourse();
        return new AssignmentResponse(
                assignment.getId(),
                course.getId(),
                course.getName(),
                assignment.getCanvasAssignmentId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getPointsPossible(),
                assignment.isCompleted(),
                assignment.isSubmitted(),
                assignment.getDifficulty(),
                assignment.getEstimatedHours(),
                assignment.getActualHours(),
                assignment.getPersonalPriority(),
                assignment.getPriorityScore(),
                assignment.getPriorityLevel(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    public PrioritizedAssignmentResponse toPrioritizedResponse(Assignment assignment, List<String> reasons) {
        AssignmentResponse base = toResponse(assignment);
        return new PrioritizedAssignmentResponse(
                base.id(),
                base.courseId(),
                base.courseName(),
                base.canvasAssignmentId(),
                base.title(),
                base.description(),
                base.dueDate(),
                base.pointsPossible(),
                base.completed(),
                base.submitted(),
                base.difficulty(),
                base.estimatedHours(),
                base.actualHours(),
                base.personalPriority(),
                base.priorityScore(),
                base.priorityLevel(),
                base.createdAt(),
                base.updatedAt(),
                reasons);
    }

    public void applyCreate(Assignment assignment, AssignmentCreateRequest request, Course course) {
        assignment.setCourse(course);
        assignment.setUserId(course.getUserId());
        assignment.setTitle(request.title().trim());
        assignment.setDescription(blankToNull(request.description()));
        assignment.setDueDate(request.dueDate());
        assignment.setPointsPossible(request.pointsPossible());
        assignment.setDifficulty(request.difficulty());
        assignment.setEstimatedHours(request.estimatedHours());
        assignment.setPersonalPriority(request.personalPriority());
        assignment.setCompleted(false);
        assignment.setSubmitted(false);
        assignment.setActualHours(0.0);
        assignment.setCanvasAssignmentId(null);
    }

    public void applyUpdate(Assignment assignment, AssignmentUpdateRequest request) {
        if (assignment.isManual()) {
            if (request.title() != null) {
                if (request.title().isBlank()) {
                    throw new IllegalArgumentException("title must not be blank");
                }
                assignment.setTitle(request.title().trim());
            }
            if (request.description() != null) {
                assignment.setDescription(blankToNull(request.description()));
            }
            if (request.dueDate() != null) {
                assignment.setDueDate(request.dueDate());
            }
            if (request.pointsPossible() != null) {
                assignment.setPointsPossible(request.pointsPossible());
            }
        }

        if (request.completed() != null) {
            assignment.setCompleted(request.completed());
        }
        if (request.difficulty() != null) {
            assignment.setDifficulty(request.difficulty());
        }
        if (request.estimatedHours() != null) {
            assignment.setEstimatedHours(request.estimatedHours());
        }
        if (request.actualHours() != null) {
            assignment.setActualHours(request.actualHours());
        }
        if (request.personalPriority() != null) {
            assignment.setPersonalPriority(request.personalPriority());
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
