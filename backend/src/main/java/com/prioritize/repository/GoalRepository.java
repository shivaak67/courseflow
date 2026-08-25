package com.prioritize.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prioritize.model.Goal;
import com.prioritize.model.GoalStatus;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, GoalStatus status);

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);
}
