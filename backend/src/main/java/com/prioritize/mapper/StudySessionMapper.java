package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.StudySessionCreateRequest;
import com.prioritize.dto.StudySessionResponse;
import com.prioritize.model.Assignment;
import com.prioritize.model.StudySession;

@Component
public class StudySessionMapper {

    public StudySessionResponse toResponse(StudySession session) {
        Assignment assignment = session.getAssignment();
        return new StudySessionResponse(
                session.getId(),
                assignment.getId(),
                assignment.getTitle(),
                assignment.getCourse().getName(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDurationMinutes(),
                session.getNotes(),
                session.getCreatedAt());
    }

    public void applyCreate(StudySession session, StudySessionCreateRequest request, Assignment assignment) {
        session.setUserId(assignment.getUserId());
        session.setAssignment(assignment);
        session.setStartedAt(request.startedAt());
        session.setEndedAt(request.endedAt());
        session.setDurationMinutes(request.durationMinutes());
        session.setNotes(blankToNull(request.notes()));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
