package com.prioritize.repository;
import com.prioritize.model.CanvasCalendarFeed;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CanvasCalendarFeedRepository extends JpaRepository<CanvasCalendarFeed, UUID> {}
