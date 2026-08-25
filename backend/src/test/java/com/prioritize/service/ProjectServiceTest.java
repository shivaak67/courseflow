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

import com.prioritize.dto.ProjectRequest;
import com.prioritize.dto.ProjectResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.ProjectMapper;
import com.prioritize.model.Category;
import com.prioritize.model.Goal;
import com.prioritize.model.Project;
import com.prioritize.model.ProjectStatus;
import com.prioritize.repository.CategoryRepository;
import com.prioritize.repository.GoalRepository;
import com.prioritize.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID GOAL_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private GoalRepository goalRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository, categoryRepository, goalRepository, new ProjectMapper());
    }

    @Test
    void createPersistsOwnedProjectWithDefaultActiveStatus() {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            if (project.getId() == null) {
                project.setId(PROJECT_ID);
            }
            if (project.getCreatedAt() == null) {
                Instant now = Instant.parse("2026-01-01T00:00:00Z");
                project.setCreatedAt(now);
                project.setUpdatedAt(now);
            }
            return project;
        });

        ProjectResponse response = projectService.create(
                USER_A,
                new ProjectRequest("Capstone", "Build the app", null, null, null, null, null));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_A);
        assertThat(captor.getValue().getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(response.title()).isEqualTo("Capstone");
        assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void createValidatesOwnedCategoryAndGoal() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
                .thenReturn(Optional.of(new Category()));
        when(goalRepository.findByIdAndUserId(GOAL_ID, USER_A))
                .thenReturn(Optional.of(new Goal()));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        projectService.create(
                USER_A,
                new ProjectRequest("Capstone", null, CATEGORY_ID, GOAL_ID, null, null, ProjectStatus.ACTIVE));

        verify(categoryRepository).findByIdAndUserId(CATEGORY_ID, USER_A);
        verify(goalRepository).findByIdAndUserId(GOAL_ID, USER_A);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createReturns404WhenCategoryNotOwned() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.create(
                        USER_A,
                        new ProjectRequest("Capstone", null, CATEGORY_ID, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(projectRepository, never()).save(any());
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(projectRepository.findByIdAndUserId(PROJECT_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.get(USER_B, PROJECT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");
    }

    @Test
    void listReturnsOnlyCallerProjects() {
        Project project = ownedProject(USER_A);
        when(projectRepository.findFiltered(USER_A, null, null)).thenReturn(List.of(project));

        List<ProjectResponse> responses = projectService.list(USER_A, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(PROJECT_ID);
        verify(projectRepository, never()).findAll();
    }

    private Project ownedProject(UUID userId) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setUserId(userId);
        project.setTitle("Capstone");
        project.setStatus(ProjectStatus.ACTIVE);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        return project;
    }
}
