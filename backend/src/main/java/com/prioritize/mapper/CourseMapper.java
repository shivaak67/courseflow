package com.prioritize.mapper;

import org.springframework.stereotype.Component;

import com.prioritize.dto.CourseRequest;
import com.prioritize.dto.CourseResponse;
import com.prioritize.model.Course;

@Component
public class CourseMapper {

    public CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCanvasCourseId(),
                course.getName(),
                course.getCourseCode(),
                course.getTerm(),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }

    public void applyCreate(Course course, CourseRequest request) {
        course.setName(request.name().trim());
        course.setCourseCode(blankToNull(request.courseCode()));
        course.setTerm(blankToNull(request.term()));
    }

    public void applyUpdate(Course course, CourseRequest request) {
        applyCreate(course, request);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
