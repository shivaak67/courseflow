package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prioritize.dto.StudySessionCreateRequest;
import com.prioritize.dto.StudySessionResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.StudySessionMapper;
import com.prioritize.model.Assignment;
import com.prioritize.model.Course;
import com.prioritize.model.StudySession;
import com.prioritize.repository.AssignmentRepository;
import com.prioritize.repository.StudySessionRepository;

@ExtendWith(MockitoExtension.class)
class StudySessionServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASSIGNMENT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SESSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    private StudySessionService studySessionService;

    @BeforeEach
    void setUp() {
        studySessionService = new StudySessionService(
                studySessionRepository, assignmentRepository, new StudySessionMapper());
    }

    @Test
    void createPersistsSessionAndIncrementsActualHours() {
        Assignment assignment = ownedAssignment(USER_A, 1.5);
        when(assignmentRepository.findByIdAndUserId(ASSIGNMENT_ID, USER_A)).thenReturn(Optional.of(assignment));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> {
            StudySession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(SESSION_ID);
            }
            if (session.getCreatedAt() == null) {
                session.setCreatedAt(Instant.parse("2026-08-23T18:00:00Z"));
            }
            return session;
        });
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant started = Instant.parse("2026-08-23T17:00:00Z");
        Instant ended = Instant.parse("2026-08-23T18:00:00Z");
        StudySessionResponse response = studySessionService.create(
                USER_A,
                new StudySessionCreateRequest(ASSIGNMENT_ID, started, ended, 90, "Focused review"));

        ArgumentCaptor<StudySession> sessionCaptor = ArgumentCaptor.forClass(StudySession.class);
        verify(studySessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUserId()).isEqualTo(USER_A);
        assertThat(sessionCaptor.getValue().getDurationMinutes()).isEqualTo(90);

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getActualHours()).isEqualTo(3.0);

        assertThat(response.id()).isEqualTo(SESSION_ID);
        assertThat(response.assignmentId()).isEqualTo(ASSIGNMENT_ID);
        assertThat(response.assignmentTitle()).isEqualTo("Homework 1");
        assertThat(response.courseName()).isEqualTo("Algorithms");
        assertThat(response.durationMinutes()).isEqualTo(90);
        assertThat(response.notes()).isEqualTo("Focused review");
    }

    @Test
    void createReturns404WhenAssignmentNotOwned() {
        when(assignmentRepository.findByIdAndUserId(ASSIGNMENT_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.create(
                        USER_B,
                        new StudySessionCreateRequest(ASSIGNMENT_ID, null, null, 60, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assignment not found");

        verify(studySessionRepository, never()).save(any());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void listReturnsOnlyCallerSessionsNewestFirst() {
        StudySession session = new StudySession();
        session.setId(SESSION_ID);
        session.setUserId(USER_A);
        session.setAssignment(ownedAssignment(USER_A, 0.0));
        session.setDurationMinutes(30);
        session.setCreatedAt(Instant.parse("2026-08-23T18:00:00Z"));

        when(studySessionRepository.findByUserIdOrderByCreatedAtDesc(USER_A)).thenReturn(List.of(session));

        List<StudySessionResponse> responses = studySessionService.list(USER_A);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(SESSION_ID);
        verify(studySessionRepository, never()).findAll();
    }

    private Assignment ownedAssignment(UUID userId, double actualHours) {
        Course course = new Course();
        course.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        course.setUserId(userId);
        course.setName("Algorithms");

        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setUserId(userId);
        assignment.setCourse(course);
        assignment.setTitle("Homework 1");
        assignment.setCompleted(false);
        assignment.setSubmitted(false);
        assignment.setActualHours(actualHours);
        return assignment;
    }
}
