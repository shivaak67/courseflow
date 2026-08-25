package com.prioritize.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prioritize.model.AuthProvider;
import com.prioritize.model.Role;
import com.prioritize.model.Task;
import com.prioritize.model.TaskPriority;
import com.prioritize.model.TaskStatus;
import com.prioritize.model.User;
import com.prioritize.repository.CalendarEventRepository;
import com.prioritize.repository.ScheduleBlockRepository;
import com.prioritize.repository.TaskRepository;
import com.prioritize.repository.UserRepository;
import com.prioritize.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScheduleCalendarControllerTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleBlockRepository scheduleBlockRepository;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String tokenA;
    private String tokenB;
    private Task taskA;

    @BeforeEach
    void setUp() {
        scheduleBlockRepository.deleteAll();
        calendarEventRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(localUser(USER_A, "alice@example.com", "Alice"));
        userRepository.save(localUser(USER_B, "bob@example.com", "Bob"));
        tokenA = jwtService.generateToken(USER_A, "alice@example.com");
        tokenB = jwtService.generateToken(USER_B, "bob@example.com");

        Task task = new Task();
        task.setUserId(USER_A);
        task.setTitle("Write essay");
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TODO);
        task.setActualMinutes(0);
        taskA = taskRepository.save(task);
    }

    @Test
    void scheduleBlockCrudAndOwnershipIsolation() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/schedule-blocks")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskId": "%s",
                                  "startAt": "2026-03-10T10:00:00Z",
                                  "endAt": "2026-03-10T11:00:00Z"
                                }
                                """.formatted(taskA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(taskA.getId().toString()))
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn();

        String blockId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/schedule-blocks")
                        .with(bearer(tokenA))
                        .param("from", "2026-03-10T00:00:00Z")
                        .param("to", "2026-03-11T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/schedule-blocks/{id}", blockId).with(bearer(tokenB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/schedule-blocks/{id}", blockId).with(bearer(tokenA)))
                .andExpect(status().isNoContent());
    }

    @Test
    void calendarEventCreateListAndRejectBadRange() throws Exception {
        mockMvc.perform(post("/api/calendar-events")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Dentist",
                                  "startAt": "2026-03-12T09:00:00Z",
                                  "endAt": "2026-03-12T10:00:00Z",
                                  "allDay": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Dentist"))
                .andExpect(jsonPath("$.allDay").value(false));

        mockMvc.perform(get("/api/calendar-events")
                        .with(bearer(tokenA))
                        .param("from", "2026-03-12T00:00:00Z")
                        .param("to", "2026-03-13T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/calendar-events").with(bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/calendar-events")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bad",
                                  "startAt": "2026-03-12T10:00:00Z",
                                  "endAt": "2026-03-12T09:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduleBlockRejectsUnownedTask() throws Exception {
        mockMvc.perform(post("/api/schedule-blocks")
                        .with(bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskId": "%s",
                                  "startAt": "2026-03-10T10:00:00Z",
                                  "endAt": "2026-03-10T11:00:00Z"
                                }
                                """.formatted(taskA.getId())))
                .andExpect(status().isNotFound());
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
