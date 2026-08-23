package com.prioritize.integration.canvas;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.canvas.mock-enabled", havingValue = "true")
public class MockCanvasClient implements CanvasClient {

    @Override
    public List<CanvasCourseData> listCourses() {
        return List.of(
                new CanvasCourseData("mock-cs101", "Intro to Computer Science", "CS 101", "Fall 2026"),
                new CanvasCourseData("mock-math201", "Calculus II", "MATH 201", "Fall 2026"));
    }

    @Override
    public List<CanvasAssignmentData> listAssignments(String canvasCourseId) {
        Instant soon = Instant.now().plus(5, ChronoUnit.DAYS);
        Instant later = Instant.now().plus(12, ChronoUnit.DAYS);
        return switch (canvasCourseId) {
            case "mock-cs101" -> List.of(
                    new CanvasAssignmentData(
                            "mock-cs101-hw1",
                            "Problem Set 1",
                            "Warm-up exercises on algorithms.",
                            soon,
                            100.0,
                            false,
                            false),
                    new CanvasAssignmentData(
                            "mock-cs101-lab1",
                            "Lab 1: Git Basics",
                            "Complete the lab worksheet.",
                            later,
                            50.0,
                            false,
                            false));
            case "mock-math201" -> List.of(
                    new CanvasAssignmentData(
                            "mock-math201-quiz1",
                            "Quiz 1: Integrals",
                            null,
                            soon.plus(2, ChronoUnit.DAYS),
                            25.0,
                            false,
                            false));
            default -> List.of();
        };
    }
}
