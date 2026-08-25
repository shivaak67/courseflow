package com.prioritize.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prioritize.model.Routine;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    Optional<Routine> findByIdAndUserId(UUID id, UUID userId);

    List<Routine> findByUserIdOrderByStartDateAsc(UUID userId);

    List<Routine> findByUserIdAndActiveOrderByStartDateAsc(UUID userId, boolean active);
}
