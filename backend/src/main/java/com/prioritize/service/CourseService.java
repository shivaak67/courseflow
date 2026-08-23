package com.prioritize.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.CourseRequest;
import com.prioritize.dto.CourseResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.CourseMapper;
import com.prioritize.model.Course;
import com.prioritize.repository.CourseRepository;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseService(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> list(UUID userId) {
        return courseRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse get(UUID userId, UUID courseId) {
        return courseMapper.toResponse(requireOwned(userId, courseId));
    }

    public CourseResponse create(UUID userId, CourseRequest request) {
        Course course = new Course();
        course.setUserId(userId);
        courseMapper.applyCreate(course, request);
        return courseMapper.toResponse(courseRepository.save(course));
    }

    public CourseResponse update(UUID userId, UUID courseId, CourseRequest request) {
        Course course = requireOwned(userId, courseId);
        courseMapper.applyUpdate(course, request);
        return courseMapper.toResponse(courseRepository.save(course));
    }

    public void delete(UUID userId, UUID courseId) {
        Course course = requireOwned(userId, courseId);
        courseRepository.delete(course);
    }

    @Transactional(readOnly = true)
    public Course requireOwned(UUID userId, UUID courseId) {
        return courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }
}
