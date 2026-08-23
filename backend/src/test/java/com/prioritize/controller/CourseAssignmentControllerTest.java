package com.prioritize.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prioritize.model.Assignment;
import com.prioritize.model.Course;
import com.prioritize.model.Difficulty;
import com.prioritize.repository.AssignmentRepository;
import com.prioritize.repository.CourseRepository;
import com.prioritize.security.CurrentUserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseAssignmentControllerTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @BeforeEach
    void cleanDatabase() {
        assignmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    void courseCrudAndOwnershipIsolation() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/courses")
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Data Structures","courseCode":"CS201","term":"Fall 2026"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Data Structures"))
                .andExpect(jsonPath("$.courseCode").value("CS201"))
                .andReturn();

        String courseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/courses")
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/courses/{id}", courseId)
                        .header(CurrentUserService.USER_ID_HEADER, USER_B.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/courses/{id}", courseId)
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Data Structures II","courseCode":"CS202","term":"Spring 2027"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Data Structures II"));

        mockMvc.perform(delete("/api/courses/{id}", courseId)
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/{id}", courseId)
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignmentFiltersUpcomingOverdueAndRejectCrossUser() throws Exception {
        Course course = new Course();
        course.setUserId(USER_A);
        course.setName("Algorithms");
        course = courseRepository.save(course);

        Assignment upcoming = new Assignment();
        upcoming.setUserId(USER_A);
        upcoming.setCourse(course);
        upcoming.setTitle("Upcoming Homework");
        upcoming.setCanvasAssignmentId("canvas-upcoming-1");
        upcoming.setDueDate(Instant.now().plusSeconds(86_400));
        upcoming.setCompleted(false);
        upcoming.setSubmitted(false);
        upcoming.setActualHours(0.0);
        upcoming.setDifficulty(Difficulty.MEDIUM);
        upcoming = assignmentRepository.save(upcoming);

        Assignment overdue = new Assignment();
        overdue.setUserId(USER_A);
        overdue.setCourse(course);
        overdue.setTitle("Overdue Lab");
        overdue.setCanvasAssignmentId("canvas-overdue-1");
        overdue.setDueDate(Instant.now().minusSeconds(86_400));
        overdue.setCompleted(false);
        overdue.setSubmitted(false);
        overdue.setActualHours(0.0);
        assignmentRepository.save(overdue);

        mockMvc.perform(post("/api/assignments")
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "title": "Manual Project",
                                  "description": "Build a tree",
                                  "difficulty": "HARD",
                                  "estimatedHours": 5.0,
                                  "personalPriority": 4
                                }
                                """.formatted(course.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Manual Project"))
                .andExpect(jsonPath("$.courseName").value("Algorithms"))
                .andExpect(jsonPath("$.difficulty").value("HARD"));

        mockMvc.perform(get("/api/assignments")
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString())
                        .param("courseId", course.getId().toString())
                        .param("completed", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/api/assignments/upcoming")
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Upcoming Homework"));

        mockMvc.perform(get("/api/assignments/overdue")
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Overdue Lab"));

        mockMvc.perform(get("/api/assignments/prioritized")
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].reasons").isArray());

        mockMvc.perform(get("/api/assignments/{id}", upcoming.getId())
                        .header(CurrentUserService.USER_ID_HEADER, USER_B.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/assignments/{id}", upcoming.getId())
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"completed": true, "actualHours": 2.5, "personalPriority": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.actualHours").value(2.5));
    }

    @Test
    void missingUserHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCourseRequiresName() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header(CurrentUserService.USER_ID_HEADER, USER_A.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseCode":"CS101"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
