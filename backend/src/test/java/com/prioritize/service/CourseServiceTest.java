package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prioritize.dto.CourseRequest;
import com.prioritize.dto.CourseResponse;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.CourseMapper;
import com.prioritize.model.Course;
import com.prioritize.repository.CourseRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COURSE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, new CourseMapper());
    }

    @Test
    void createPersistsOwnedCourse() {
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            if (course.getId() == null) {
                course.setId(COURSE_ID);
            }
            if (course.getCreatedAt() == null) {
                Instant now = Instant.parse("2026-01-01T00:00:00Z");
                course.setCreatedAt(now);
                course.setUpdatedAt(now);
            }
            return course;
        });

        CourseResponse response = courseService.create(USER_A, new CourseRequest("Algorithms", "CS301", "Fall 2026"));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_A);
        assertThat(response.name()).isEqualTo("Algorithms");
        assertThat(response.courseCode()).isEqualTo("CS301");
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(courseRepository.findByIdAndUserId(COURSE_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.get(USER_B, COURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course not found");
    }

    @Test
    void listReturnsOnlyCallerCourses() {
        Course course = ownedCourse(USER_A);
        when(courseRepository.findByUserIdOrderByNameAsc(USER_A)).thenReturn(List.of(course));

        List<CourseResponse> responses = courseService.list(USER_A);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(COURSE_ID);
        verify(courseRepository, never()).findAll();
    }

    private Course ownedCourse(UUID userId) {
        Course course = new Course();
        course.setId(COURSE_ID);
        course.setUserId(userId);
        course.setName("Algorithms");
        course.setCourseCode("CS301");
        course.setTerm("Fall 2026");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        course.setCreatedAt(now);
        course.setUpdatedAt(now);
        return course;
    }
}
