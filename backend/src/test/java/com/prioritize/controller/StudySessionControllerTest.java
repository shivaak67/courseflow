package com.prioritize.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.prioritize.model.Assignment;
import com.prioritize.model.AuthProvider;
import com.prioritize.model.Course;
import com.prioritize.model.Role;
import com.prioritize.model.User;
import com.prioritize.repository.AssignmentRepository;
import com.prioritize.repository.CourseRepository;
import com.prioritize.repository.StudySessionRepository;
import com.prioritize.repository.UserRepository;
import com.prioritize.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudySessionControllerTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String tokenA;
    private String tokenB;
    private Assignment assignmentA;

    @BeforeEach
    void setUp() {
        studySessionRepository.deleteAll();
        assignmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(localUser(USER_A, "alice@example.com", "Alice"));
        userRepository.save(localUser(USER_B, "bob@example.com", "Bob"));
        tokenA = jwtService.generateToken(USER_A, "alice@example.com");
        tokenB = jwtService.generateToken(USER_B, "bob@example.com");

        Course course = new Course();
        course.setUserId(USER_A);
        course.setName("Algorithms");
        course = courseRepository.save(course);

        Assignment assignment = new Assignment();
        assignment.setUserId(USER_A);
        assignment.setCourse(course);
        assignment.setTitle("Homework 1");
        assignment.setCompleted(false);
        assignment.setSubmitted(false);
        assignment.setActualHours(0.0);
        assignmentA = assignmentRepository.save(assignment);
    }

    @Test
    void createAndListOwnedSessionsAndRejectCrossUserAssignment() throws Exception {
        mockMvc.perform(post("/api/study-sessions")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignmentId": "%s",
                                  "startedAt": "2026-08-23T17:00:00Z",
                                  "endedAt": "2026-08-23T18:00:00Z",
                                  "durationMinutes": 60,
                                  "notes": "Pomodoro"
                                }
                                """.formatted(assignmentA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").value(assignmentA.getId().toString()))
                .andExpect(jsonPath("$.assignmentTitle").value("Homework 1"))
                .andExpect(jsonPath("$.courseName").value("Algorithms"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.notes").value("Pomodoro"));

        mockMvc.perform(get("/api/assignments/{id}", assignmentA.getId()).with(bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualHours").value(1.0));

        mockMvc.perform(get("/api/study-sessions").with(bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].assignmentTitle").value("Homework 1"));

        mockMvc.perform(get("/api/study-sessions").with(bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/study-sessions")
                        .with(bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignmentId": "%s",
                                  "durationMinutes": 30
                                }
                                """.formatted(assignmentA.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRequiresDurationMinutesAtLeastOne() throws Exception {
        mockMvc.perform(post("/api/study-sessions")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignmentId": "%s",
                                  "durationMinutes": 0
                                }
                                """.formatted(assignmentA.getId())))
                .andExpect(status().isBadRequest());
    }

    private static User localUser(UUID id, String email, String firstName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName("Test");
        user.setEmail(email);
        user.setPasswordHash("$2a$10$placeholderhashnotusedforjwttestsxxxxxx");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(Role.USER);
        return user;
    }

    private static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }
}
