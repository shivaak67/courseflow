package com.prioritize.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prioritize.model.StudySession;

public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    @Query("""
            SELECT s FROM StudySession s
            JOIN FETCH s.assignment a
            JOIN FETCH a.course
            WHERE s.userId = :userId
            ORDER BY s.createdAt DESC
            """)
    List<StudySession> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("""
            SELECT s FROM StudySession s
            JOIN FETCH s.assignment a
            JOIN FETCH a.course
            WHERE s.id = :id AND s.userId = :userId
            """)
    Optional<StudySession> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
