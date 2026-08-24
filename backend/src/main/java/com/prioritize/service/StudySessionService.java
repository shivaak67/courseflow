package com.prioritize.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.StudySessionCreateRequest;
import com.prioritize.dto.StudySessionResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.StudySessionMapper;
import com.prioritize.model.Assignment;
import com.prioritize.model.StudySession;
import com.prioritize.repository.AssignmentRepository;
import com.prioritize.repository.StudySessionRepository;

@Service
@Transactional
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudySessionMapper studySessionMapper;

    public StudySessionService(
            StudySessionRepository studySessionRepository,
            AssignmentRepository assignmentRepository,
            StudySessionMapper studySessionMapper) {
        this.studySessionRepository = studySessionRepository;
        this.assignmentRepository = assignmentRepository;
        this.studySessionMapper = studySessionMapper;
    }

    public StudySessionResponse create(UUID userId, StudySessionCreateRequest request) {
        Assignment assignment = assignmentRepository.findByIdAndUserId(request.assignmentId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        StudySession session = new StudySession();
        studySessionMapper.applyCreate(session, request, assignment);
        StudySession saved = studySessionRepository.save(session);

        double hours = request.durationMinutes() / 60.0;
        Double current = assignment.getActualHours();
        assignment.setActualHours((current == null ? 0.0 : current) + hours);
        assignmentRepository.save(assignment);

        return studySessionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> list(UUID userId) {
        return studySessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(studySessionMapper::toResponse)
                .toList();
    }
}
