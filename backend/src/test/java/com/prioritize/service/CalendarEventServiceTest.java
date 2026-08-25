package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.prioritize.dto.CalendarEventRequest;
import com.prioritize.dto.CalendarEventResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.CalendarEventMapper;
import com.prioritize.model.CalendarEvent;
import com.prioritize.model.Category;
import com.prioritize.repository.CalendarEventRepository;
import com.prioritize.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EVENT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final Instant START = Instant.parse("2026-03-01T14:00:00Z");
    private static final Instant END = Instant.parse("2026-03-01T15:30:00Z");

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private CalendarEventService calendarEventService;

    @BeforeEach
    void setUp() {
        calendarEventService = new CalendarEventService(
                calendarEventRepository, categoryRepository, new CalendarEventMapper());
    }

    @Test
    void createPersistsOwnedEvent() {
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
            CalendarEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(EVENT_ID);
            }
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            if (event.getCreatedAt() == null) {
                event.setCreatedAt(now);
                event.setUpdatedAt(now);
            }
            return event;
        });

        CalendarEventResponse response = calendarEventService.create(
                USER_A,
                new CalendarEventRequest("Office hours", "Room 12", null, START, END, null));

        ArgumentCaptor<CalendarEvent> captor = ArgumentCaptor.forClass(CalendarEvent.class);
        verify(calendarEventRepository).save(captor.capture());
        CalendarEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_A);
        assertThat(saved.getTitle()).isEqualTo("Office hours");
        assertThat(saved.isAllDay()).isFalse();
        assertThat(response.id()).isEqualTo(EVENT_ID);
        assertThat(response.title()).isEqualTo("Office hours");
    }

    @Test
    void createRejectsEndBeforeOrEqualStart() {
        assertThatThrownBy(() -> calendarEventService.create(
                        USER_A,
                        new CalendarEventRequest("Bad", null, null, START, START, false)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessage("endAt must be after startAt");

        verify(calendarEventRepository, never()).save(any());
    }

    @Test
    void createReturns404WhenCategoryNotOwned() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.create(
                        USER_A,
                        new CalendarEventRequest("Meeting", null, CATEGORY_ID, START, END, false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(calendarEventRepository, never()).save(any());
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(calendarEventRepository.findByIdAndUserId(EVENT_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarEventService.get(USER_B, EVENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Calendar event not found");
    }

    @Test
    void createWithOwnedCategorySucceeds() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
                .thenReturn(Optional.of(new Category()));
        when(calendarEventRepository.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
            CalendarEvent event = invocation.getArgument(0);
            event.setId(EVENT_ID);
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            event.setCreatedAt(now);
            event.setUpdatedAt(now);
            return event;
        });

        CalendarEventResponse response = calendarEventService.create(
                USER_A,
                new CalendarEventRequest("Study", null, CATEGORY_ID, START, END, true));

        assertThat(response.categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(response.allDay()).isTrue();
    }
}
