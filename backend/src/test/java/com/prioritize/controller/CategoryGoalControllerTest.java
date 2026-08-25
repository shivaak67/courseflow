package com.prioritize.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.prioritize.model.User;
import com.prioritize.repository.CategoryRepository;
import com.prioritize.repository.GoalRepository;
import com.prioritize.repository.UserRepository;
import com.prioritize.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryGoalControllerTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        goalRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(localUser(USER_A, "alice@example.com", "Alice"));
        userRepository.save(localUser(USER_B, "bob@example.com", "Bob"));
        tokenA = jwtService.generateToken(USER_A, "alice@example.com");
        tokenB = jwtService.generateToken(USER_B, "bob@example.com");
    }

    @Test
    void categoryCrudOwnershipIsolationAndDuplicateName() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/categories")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Academics","icon":"book","color":"#336699"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Academics"))
                .andExpect(jsonPath("$.icon").value("book"))
                .andExpect(jsonPath("$.color").value("#336699"))
                .andReturn();

        String categoryId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/categories").with(bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/categories/{id}", categoryId).with(bearer(tokenB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/categories")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"academics"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"School","icon":"grad","color":"#112233"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("School"));

        mockMvc.perform(delete("/api/categories/{id}", categoryId).with(bearer(tokenA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/{id}", categoryId).with(bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void goalCrudOwnershipIsolationAndStatusFilter() throws Exception {
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Career"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        MvcResult createResult = mockMvc.perform(post("/api/goals")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "title": "Land internship",
                                  "description": "Apply widely",
                                  "targetDate": "2026-06-01"
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Land internship"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andReturn();

        String goalId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(post("/api/goals")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Paused goal","status":"PAUSED"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/goals").with(bearer(tokenA)).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Land internship"));

        mockMvc.perform(get("/api/goals/{id}", goalId).with(bearer(tokenB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/goals")
                        .with(bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","title":"Steal category"}
                                """.formatted(categoryId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/goals/{id}", goalId)
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "title": "Land summer internship",
                                  "description": "Focus on backend roles",
                                  "targetDate": "2026-05-15",
                                  "status": "COMPLETED"
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Land summer internship"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(delete("/api/goals/{id}", goalId).with(bearer(tokenA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goals/{id}", goalId).with(bearer(tokenA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRequiresRequiredFields() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"icon":"book"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/goals")
                        .with(bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"missing title"}
                                """))
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
