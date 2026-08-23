package com.prioritize.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prioritize.model.Course;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByUserIdOrderByNameAsc(UUID userId);

    Optional<Course> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    Optional<Course> findByUserIdAndCanvasCourseId(UUID userId, String canvasCourseId);
}
