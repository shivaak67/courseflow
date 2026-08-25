package com.prioritize.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.GoalRequest;
import com.prioritize.dto.GoalResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.GoalMapper;
import com.prioritize.model.Goal;
import com.prioritize.model.GoalStatus;
import com.prioritize.repository.GoalRepository;

@Service
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final CategoryService categoryService;
    private final GoalMapper goalMapper;

    public GoalService(
            GoalRepository goalRepository,
            CategoryService categoryService,
            GoalMapper goalMapper) {
        this.goalRepository = goalRepository;
        this.categoryService = categoryService;
        this.goalMapper = goalMapper;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID userId, GoalStatus status) {
        List<Goal> goals = status == null
                ? goalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return goals.stream().map(goalMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse get(UUID userId, UUID goalId) {
        return goalMapper.toResponse(requireOwned(userId, goalId));
    }

    public GoalResponse create(UUID userId, GoalRequest request) {
        validateCategoryOwnership(userId, request.categoryId());
        Goal goal = new Goal();
        goal.setUserId(userId);
        goalMapper.applyCreate(goal, request);
        return goalMapper.toResponse(goalRepository.save(goal));
    }

    public GoalResponse update(UUID userId, UUID goalId, GoalRequest request) {
        Goal goal = requireOwned(userId, goalId);
        validateCategoryOwnership(userId, request.categoryId());
        goalMapper.applyUpdate(goal, request);
        return goalMapper.toResponse(goalRepository.save(goal));
    }

    public void delete(UUID userId, UUID goalId) {
        Goal goal = requireOwned(userId, goalId);
        goalRepository.delete(goal);
    }

    @Transactional(readOnly = true)
    public Goal requireOwned(UUID userId, UUID goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
    }

    private void validateCategoryOwnership(UUID userId, UUID categoryId) {
        if (categoryId != null) {
            categoryService.requireOwned(userId, categoryId);
        }
    }
}
