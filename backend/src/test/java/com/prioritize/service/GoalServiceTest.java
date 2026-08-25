package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prioritize.dto.GoalRequest;
import com.prioritize.dto.GoalResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.GoalMapper;
import com.prioritize.model.Category;
import com.prioritize.model.Goal;
import com.prioritize.model.GoalStatus;
import com.prioritize.repository.GoalRepository;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GOAL_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private CategoryService categoryService;

    private GoalService goalService;

    @BeforeEach
    void setUp() {
        goalService = new GoalService(goalRepository, categoryService, new GoalMapper());
    }

    @Test
    void createDefaultsStatusToActiveAndValidatesCategory() {
        Category owned = new Category();
        owned.setId(CATEGORY_ID);
        when(categoryService.requireOwned(USER_A, CATEGORY_ID)).thenReturn(owned);
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal goal = invocation.getArgument(0);
            if (goal.getId() == null) {
                goal.setId(GOAL_ID);
            }
            if (goal.getCreatedAt() == null) {
                Instant now = Instant.parse("2026-01-01T00:00:00Z");
                goal.setCreatedAt(now);
                goal.setUpdatedAt(now);
            }
            return goal;
        });

        GoalResponse response = goalService.create(
                USER_A,
                new GoalRequest(CATEGORY_ID, "Finish thesis", "Draft chapters", LocalDate.of(2026, 12, 1), null));

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_A);
        assertThat(captor.getValue().getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(response.title()).isEqualTo("Finish thesis");
        assertThat(response.categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(response.status()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(goalRepository.findByIdAndUserId(GOAL_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.get(USER_B, GOAL_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Goal not found");
    }

    @Test
    void createRejectsCategoryOwnedByAnotherUser() {
        when(categoryService.requireOwned(USER_A, CATEGORY_ID))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        assertThatThrownBy(() -> goalService.create(
                USER_A, new GoalRequest(CATEGORY_ID, "Goal", null, null, GoalStatus.ACTIVE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
        verify(goalRepository, never()).save(any());
    }

    @Test
    void listFiltersByStatusWhenProvided() {
        Goal goal = ownedGoal(USER_A);
        when(goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER_A, GoalStatus.ACTIVE))
                .thenReturn(List.of(goal));

        List<GoalResponse> responses = goalService.list(USER_A, GoalStatus.ACTIVE);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(GOAL_ID);
        verify(goalRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
    }

    private Goal ownedGoal(UUID userId) {
        Goal goal = new Goal();
        goal.setId(GOAL_ID);
        goal.setUserId(userId);
        goal.setCategoryId(CATEGORY_ID);
        goal.setTitle("Finish thesis");
        goal.setStatus(GoalStatus.ACTIVE);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        goal.setCreatedAt(now);
        goal.setUpdatedAt(now);
        return goal;
    }
}
